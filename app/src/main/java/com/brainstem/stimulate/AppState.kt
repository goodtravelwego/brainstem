package com.brainstem.stimulate

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.*
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class Screen {
    HOME,
    QUICK,
    PROGRAM,
    BUILDER,
    SETTINGS,
    HISTORY,
    SUMMARY,
}

class AppState(app: Application) : AndroidViewModel(app) {
    val repo = Repository(app)
    val auth = Auth(app)
    var loggedIn by mutableStateOf(auth.hasSession() || repo.prefs.getBoolean("guest", false))
    var busy by mutableStateOf(false)
    var error by mutableStateOf("")
    var message by mutableStateOf("")
    var screen by mutableStateOf(Screen.HOME)
    var beforeSettings = Screen.HOME
    var category by mutableStateOf("relax")
    var pro by mutableStateOf(false)
    var speed by mutableIntStateOf(20)
    var theme by mutableStateOf("system")
    var spec by mutableStateOf(Spec())
    var level by mutableIntStateOf(1)
    var closedLoop by mutableStateOf(false)
    var battery by mutableIntStateOf(92)
    var profiles = mutableStateListOf<Profile>()
    var currentProfile by mutableStateOf("")
    var customs = mutableStateListOf<Program>()
    var favorites = mutableStateListOf<String>()
    var queue = mutableStateListOf<QueueItem>()
    var history = mutableStateListOf<SessionRecord>()
    var quick by mutableStateOf(Step())
    var quickMinutes by mutableIntStateOf(10)
    var selected by mutableStateOf<Program?>(null)
    var programMinutes by mutableIntStateOf(10)
    var selectedStep by mutableIntStateOf(0)
    var draft by mutableStateOf<Program?>(null)
    var editStep by mutableIntStateOf(0)
    var buildFromNew = true
    var engine by mutableStateOf<SessionEngine?>(null)
    var revision by mutableIntStateOf(0)
    var summary by mutableStateOf<SessionRecord?>(null)
    var checkOpen by mutableStateOf(false)
    var profileOpen by mutableStateOf(false)
    var profileEditing by mutableStateOf<Profile?>(null)
    var profileFormOpen by mutableStateOf(false)
    var mailEmail by mutableStateOf("")
    var mailKind by mutableStateOf("")
    private var scopeKey = "guest"
    val user
        get() = profiles.find { it.id == currentProfile } ?: profiles.firstOrNull()

    val programs
        get() = repo.presets + customs

    val actualLevel
        get() = if (pro) level else 1

    init {
        load()
        if (auth.hasSession()) loadCloudProfiles()
    }

    private fun dataKey() = "data.$scopeKey"

    private fun profileKey() = "$scopeKey.$currentProfile"

    private fun load() {
        scopeKey = repo.prefs.getString("scope", "guest") ?: "guest"
        val j = repo.read(dataKey())
        pro = j.optBoolean("pro")
        speed = j.optInt("speed", 20)
        theme = j.optString("theme", "system")
        spec =
            j.optJSONObject("spec")?.let {
                Spec(
                    it.optInt("wl", 810),
                    it.optInt("irr", 40),
                    it.optInt("duty", 50),
                    it.optInt("daymax", 40),
                )
            } ?: Spec()
        profiles.clear()
        profiles.addAll(
            j.optJSONArray("profiles")?.objects()?.map(::profileFrom)
                ?: listOf(
                    Profile("P-001", "น้องต้นกล้า", 8),
                    Profile("P-002", "คุณสมศรี (ตัวอย่าง)", 72, type = "care"),
                )
        )
        currentProfile = j.optString("current", profiles.firstOrNull()?.id.orEmpty())
        customs.clear()
        customs.addAll(j.optJSONArray("customs")?.objects()?.map(::programFrom) ?: emptyList())
        favorites.clear()
        favorites.addAll(j.optJSONArray("favorites")?.strings() ?: emptyList())
        quick = j.optJSONObject("quick")?.let(::stepFrom) ?: Step()
        quickMinutes = j.optInt("quickMin", 10)
        loadUser()
        val pending = repo.read("run.${profileKey()}")
        runCatching {
                if (pending.has("programs")) {
                    val plan =
                        makePlan(
                            pending.getJSONArray("programs").objects().map(::programFrom),
                            pending.getInt("level"),
                            pending.getBoolean("cl"),
                            pending.getBoolean("fromQueue"),
                        )
                    engine =
                        SessionEngine(plan, spec, pending.optLong("seed", 0L)).apply {
                            advance(pending.getDouble("elapsed"))
                            paused = true
                        }
                }
            }
            .onFailure { repo.put("run.${profileKey()}", JSONObject()) }
    }

    private fun loadUser() {
        val j = repo.read("user.${profileKey()}")
        level = j.optInt("level", 1).coerceIn(1, 10)
        battery = j.optInt("battery", 92)
        closedLoop = false
        queue.clear()
        queue.addAll(
            j.optJSONArray("queue")
                ?.objects()
                ?.map { QueueItem(it.getString("pid"), it.getInt("min")) }
                ?.filter { q -> programs.any { it.id == q.pid } } ?: emptyList()
        )
        history.clear()
        history.addAll(j.optJSONArray("history")?.objects()?.map(::recordFrom) ?: emptyList())
    }

    fun save() {
        repo.prefs.edit { putString("scope", scopeKey) }
        repo.put(
            dataKey(),
            JSONObject()
                .put("pro", pro)
                .put("speed", speed)
                .put("theme", theme)
                .put(
                    "spec",
                    JSONObject()
                        .put("wl", spec.wavelength)
                        .put("irr", spec.irradiance)
                        .put("duty", spec.duty)
                        .put("daymax", spec.dailyMax),
                )
                .put("profiles", JSONArray(profiles.map { it.json() }))
                .put("current", currentProfile)
                .put("customs", JSONArray(customs.map { it.json() }))
                .put("favorites", JSONArray(favorites))
                .put("quick", quick.json())
                .put("quickMin", quickMinutes),
        )
        repo.put(
            "user.${profileKey()}",
            JSONObject()
                .put("level", level)
                .put("battery", battery)
                .put(
                    "queue",
                    JSONArray(queue.map { JSONObject().put("pid", it.pid).put("min", it.minutes) }),
                )
                .put("history", JSONArray(history.takeLast(300).map { it.json() })),
        )
        val r = engine
        repo.put(
            "run.${profileKey()}",
            if (r == null) JSONObject()
            else
                JSONObject()
                    .put("programs", JSONArray(r.plan.programs.map { it.json() }))
                    .put("level", r.plan.level)
                    .put("cl", r.plan.closedLoop)
                    .put("fromQueue", r.plan.fromQueue)
                    .put("seed", r.seed)
                    .put("elapsed", r.elapsed),
        )
    }

    fun guest() {
        scopeKey = "guest"
        repo.prefs.edit {
            putBoolean("guest", true)
            putString("scope", scopeKey)
        }
        load()
        loggedIn = true
    }

    fun signIn(email: String, password: String, signup: Boolean = false) {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            error = "อีเมลไม่ถูกต้อง"
            return
        }
        if (password.length < 6) {
            error = "รหัสผ่านต้องมีอย่างน้อย 6 ตัว"
            return
        }
        viewModelScope.launch {
            busy = true
            error = ""
            try {
                if (signup && !auth.signUp(email.trim(), password)) {
                    mailEmail = email
                    mailKind = "confirm"
                    return@launch
                } else if (!signup) auth.signIn(email.trim(), password)
                afterLogin()
            } catch (e: Exception) {
                error = e.message ?: "ต่อเน็ตไม่ได้ · ลองใหม่อีกครั้ง"
            } finally {
                busy = false
            }
        }
    }

    fun recover(email: String) {
        if (email.isBlank()) {
            error = "ใส่อีเมลก่อน แล้วกด “ลืมรหัสผ่าน?” อีกครั้ง"
            return
        }
        viewModelScope.launch {
            busy = true
            try {
                auth.recover(email.trim())
                mailEmail = email
                mailKind = "reset"
            } catch (e: Exception) {
                error = e.message.orEmpty()
            } finally {
                busy = false
            }
        }
    }

    fun callback(uri: Uri) {
        viewModelScope.launch {
            busy = true
            try {
                auth.callback(uri)
                afterLogin()
            } catch (e: Exception) {
                error = e.message.orEmpty()
                loggedIn = false
            } finally {
                busy = false
            }
        }
    }

    private suspend fun afterLogin() {
        val fetched = auth.profiles()
        scopeKey = auth.accountId()
        repo.prefs.edit {
            putString("scope", scopeKey)
            putBoolean("guest", false)
        }
        load()
        val cloud =
            fetched.map { p ->
                profiles
                    .find { it.cloud == p.cloud }
                    ?.let { old -> p.copy(id = old.id, photo = p.photo.ifBlank { old.photo }) } ?: p
            }
        val local =
            profiles.filter {
                it.cloud.isBlank() &&
                    (it.name !in listOf("น้องต้นกล้า", "คุณสมศรี (ตัวอย่าง)") ||
                        (repo.read("user.$scopeKey.${it.id}").optJSONArray("history")?.length()
                            ?: 0) > 0)
            }
        if (cloud.isNotEmpty() || local.isNotEmpty()) {
            profiles.clear()
            profiles.addAll(cloud + local)
            if (profiles.none { it.id == currentProfile }) currentProfile = profiles.first().id
            loadUser()
        } else {
            profiles.clear()
            currentProfile = ""
            profileOpen = true
            profileFormOpen = true
        }
        loggedIn = true
        save()
    }

    private fun loadCloudProfiles() {
        viewModelScope.launch {
            try {
                afterLogin()
            } catch (e: Exception) {
                message = "โหลดโปรไฟล์ออนไลน์ไม่สำเร็จ · ${e.message.orEmpty()}"
            }
        }
    }

    fun logout() {
        save()
        viewModelScope.launch {
            try {
                auth.signOut()
            } catch (_: Exception) {}
            repo.prefs.edit { putBoolean("guest", false) }
            loggedIn = false
            mailEmail = ""
            profileOpen = false
            profileFormOpen = false
            profileEditing = null
            screen = Screen.HOME
        }
    }

    fun switchProfile(id: String) {
        if (engine != null) return
        save()
        currentProfile = id
        loadUser()
        save()
        profileOpen = false
    }

    fun addLocalProfile(name: String, age: Int) {
        if (name.isBlank()) return
        val p = Profile("P-${System.currentTimeMillis()}", name.trim(), age.coerceIn(3, 110))
        save()
        profiles.add(p)
        currentProfile = p.id
        loadUser()
        save()
        profileOpen = false
    }

    fun editProfile(profile: Profile?) {
        profileEditing = profile
        profileFormOpen = true
        error = ""
    }

    fun nextProfileId(): String {
        var number = profiles.size + 1
        var id: String
        do {
            id = "P-${(number++).toString().padStart(3,'0')}"
        } while (profiles.any { it.id == id })
        return id
    }

    fun saveProfile(profile: Profile) {
        viewModelScope.launch {
            busy = true
            error = ""
            try {
                val saved = if (auth.hasSession()) auth.saveProfile(profile) else profile
                if (auth.hasSession() && auth.photoUploadFailed)
                    android.widget.Toast.makeText(
                            getApplication(),
                            "อัปโหลดรูปไม่สำเร็จ · เก็บรูปไว้ในเครื่อง",
                            android.widget.Toast.LENGTH_LONG,
                        )
                        .show()
                save()
                val i = profiles.indexOfFirst { it.id == saved.id }
                if (i >= 0) profiles[i] = saved else profiles.add(saved)
                currentProfile = saved.id
                loadUser()
                profileFormOpen = false
                profileOpen = false
                profileEditing = null
                save()
            } catch (e: Exception) {
                error = e.message.orEmpty()
            } finally {
                busy = false
            }
        }
    }

    fun togglePro() {
        pro = !pro
        save()
    }

    fun navigate(s: Screen) {
        if (s == Screen.SETTINGS) beforeSettings = screen
        screen = s
        error = ""
    }

    fun back() {
        screen =
            when (screen) {
                Screen.SETTINGS -> beforeSettings
                Screen.BUILDER -> if (buildFromNew) Screen.HOME else Screen.PROGRAM
                else -> Screen.HOME
            }
        error = ""
    }

    fun find(id: String) = programs.find { it.id == id }

    fun open(p: Program) {
        selected = p
        selectedStep = 0
        programMinutes = p.minutes
        screen = Screen.PROGRAM
    }

    fun toggleFavorite(id: String) {
        if (favorites.contains(id)) favorites.remove(id) else favorites.add(0, id)
        save()
    }

    fun toggleQueue(p: Program) {
        val i = queue.indexOfFirst { it.pid == p.id }
        if (i < 0) queue.add(QueueItem(p.id, p.minutes)) else queue.removeAt(i)
        save()
    }

    fun changeQueue(i: Int, delta: Int) {
        queue[i] = queue[i].copy(minutes = (queue[i].minutes + delta).coerceIn(1, 60))
        save()
    }

    fun moveQueue(i: Int) {
        if (i > 0) {
            val q = queue.removeAt(i)
            queue.add(i - 1, q)
            save()
        }
    }

    private fun checkKey() = "headset.$scopeKey.$currentProfile.${java.time.LocalDate.now()}"

    fun wasHeadsetCheckedToday() = repo.read(checkKey()).has("date")

    fun requestClosedLoop() {
        if (closedLoop) {
            closedLoop = false
            save()
        } else checkOpen = true
    }

    fun checkComplete(values: Map<String, Double> = emptyMap()) {
        repo.put(checkKey(), JSONObject(values).put("date", System.currentTimeMillis()))
        checkOpen = false
        closedLoop = true
        save()
    }

    fun quickProgram(): Program {
        val s = quick.copy(dur = quickMinutes * 60)
        return Program(
            "quick",
            "Quick · ${s.frequencyText}",
            goal = "${areaName(s.pts)} · ${s.frequencyText}",
            steps = listOf(s.copy(label = "${areaName(s.pts)} ${s.frequencyText}")),
        )
    }

    fun saveQuick() {
        val p =
            quickProgram().let { it.copy(id = "c-${System.currentTimeMillis()}", name = it.goal) }
        customs.add(p)
        category = "custom"
        screen = Screen.HOME
        save()
    }

    fun start(programs: List<Program>, fromQueue: Boolean = false) {
        if (programs.isEmpty() || programs.any { it.steps.any { s -> s.pts.isEmpty() } }) return
        engine = SessionEngine(makePlan(programs, actualLevel, closedLoop, fromQueue), spec)
        revision++
        save()
    }

    fun tick(seconds: Double) {
        val r = engine ?: return
        r.advance(seconds)
        revision++
        if (r.finished) finish(true)
    }

    fun pause() {
        engine?.let {
            it.paused = !it.paused
            revision++
            save()
        }
    }

    fun pauseInBackground() {
        engine?.let {
            it.paused = true
            revision++
            save()
        }
    }

    fun finish(complete: Boolean) {
        val r = engine ?: return
        if (!complete) r.endEarly()
        engine = null
        revision++
        if (r.elapsed >= 5) {
            val record = r.record(complete)
            history.add(record)
            summary = record
            screen = Screen.SUMMARY
            battery = (battery - maxOf(1, (r.elapsed / 300).roundToInt())).coerceAtLeast(5)
            if (complete && r.plan.fromQueue) queue.clear()
        }
        save()
    }

    fun newProgram() {
        draft =
            Program(
                "c-${System.currentTimeMillis()}",
                "โปรแกรมใหม่",
                steps = listOf(Step(dur = 300)),
            )
        editStep = 0
        buildFromNew = true
        screen = Screen.BUILDER
    }

    fun editProgram(copy: Boolean) {
        val p = selected ?: return
        draft =
            if (copy)
                p.copy(
                    id = "c-${System.currentTimeMillis()}",
                    name = p.name + " (copy)",
                    src = "custom",
                    cat = "custom",
                )
            else p.copy()
        editStep = selectedStep
        buildFromNew = false
        screen = Screen.BUILDER
    }

    fun updateStep(s: Step) {
        val d = draft ?: return
        draft =
            d.copy(
                steps =
                    d.steps.toMutableList().apply { this[editStep] = s.copy(label = s.autoLabel()) }
            )
    }

    fun builderAction(action: String, i: Int = editStep) {
        val d = draft ?: return
        val list = d.steps.toMutableList()
        when (action) {
            "up" ->
                if (i > 0) {
                    val a = list.removeAt(i)
                    list.add(i - 1, a)
                    editStep = i - 1
                }
            "down" ->
                if (i < list.lastIndex) {
                    val a = list.removeAt(i)
                    list.add(i + 1, a)
                    editStep = i + 1
                }
            "duplicate" -> {
                list.add(i + 1, list[i].copy())
                editStep = i + 1
            }
            "delete" ->
                if (list.size > 1) {
                    list.removeAt(i)
                    editStep = maxOf(0, i - 1)
                }
            "add" -> {
                list.add(Step(pts = list.last().pts, dur = 300, power = list.last().power))
                editStep = list.lastIndex
            }
        }
        draft = d.copy(steps = list)
    }

    fun saveDraft() {
        val p = draft ?: return
        if (p.steps.any { it.pts.isEmpty() }) {
            error = "มีขั้นที่ยังไม่ได้เลือกจุดปล่อยแสง"
            return
        }
        if (p.name.isBlank()) {
            error = "กรุณาใส่ชื่อโปรแกรม"
            return
        }
        customs.removeAll { it.id == p.id }
        customs.add(p)
        save()
        open(p)
    }

    fun deleteProgram() {
        val p = selected ?: return
        customs.removeAll { it.id == p.id }
        queue.removeAll { it.pid == p.id }
        favorites.remove(p.id)
        screen = Screen.HOME
        save()
    }

    fun repeatSession(r: SessionRecord) {
        queue.clear()
        queue.addAll(r.items.filter { find(it.pid) != null }.map { QueueItem(it.pid, it.minutes) })
        screen = Screen.HOME
        save()
    }

    fun recentSessions(): List<SessionRecord> =
        history
            .asReversed()
            .mapNotNull { record ->
                val valid = record.items.filter { find(it.pid) != null }
                if (valid.isEmpty()) null else record.copy(items = valid)
            }
            .distinctBy { it.items.map { item -> item.pid to item.minutes } }
            .take(3)
}
