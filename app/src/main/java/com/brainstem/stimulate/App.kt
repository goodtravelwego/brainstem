@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.brainstem.stimulate

import androidx.activity.compose.BackHandler
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import kotlin.math.*

@Composable
fun BrainApp(a: AppState) {
    val context = LocalContext.current
    val p = C
    BackHandler(
        enabled =
            a.loggedIn &&
                (a.screen != Screen.HOME || a.engine != null || a.checkOpen || a.profileOpen)
    ) {
        when {
            a.checkOpen -> a.checkOpen = false
            a.profileOpen -> a.profileOpen = false
            a.engine != null -> a.pause()
            else -> a.back()
        }
    }
    Box(Modifier.fillMaxSize().background(p.bg).windowInsetsPadding(WindowInsets.safeDrawing)) {
        if (!a.loggedIn)
            LoginScreen(a) {
                runCatching {
                        CustomTabsIntent.Builder().build().launchUrl(context, a.auth.googleUrl())
                    }
                    .onFailure { a.error = it.message.orEmpty() }
            }
        else if (a.engine != null) RunScreen(a)
        else
            Column(Modifier.fillMaxSize()) {
                AppHeader(a)
                Box(Modifier.weight(1f)) {
                    when (a.screen) {
                        Screen.HOME -> HomeScreen(a)
                        Screen.QUICK -> QuickScreen(a)
                        Screen.PROGRAM -> ProgramScreen(a)
                        Screen.BUILDER -> BuilderScreen(a)
                        Screen.SETTINGS -> SettingsScreen(a)
                        Screen.HISTORY -> HistoryScreen(a)
                        Screen.SUMMARY -> SummaryScreen(a)
                    }
                }
            }
        if (a.checkOpen) HeadsetCheck(a)
        if (a.profileOpen && !a.profileFormOpen) ProfileDialog(a)
        if (a.profileFormOpen) ProfileForm(a)
        if (a.message.isNotBlank())
            AlertDialog(
                onDismissRequest = { a.message = "" },
                title = { Title("BrainSTEM") },
                text = { Label(a.message) },
                confirmButton = {
                    TextButton(onClick = { a.message = "" }) { Label("ตกลง", color = p.accent) }
                },
            )
    }
}

@Composable
private fun AppHeader(a: AppState) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp).heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (a.screen != Screen.HOME)
            Box(
                Modifier.size(36.dp)
                    .clip(CircleShape)
                    .background(C.surface)
                    .clickable { a.back() }
                    .semantics { contentDescription = "ย้อนกลับ" },
                contentAlignment = Alignment.Center,
            ) {
                LineIcon("back", Modifier.size(19.dp), C.ink)
            }
        if (a.screen == Screen.HOME) {
            Wordmark()
            Label("PBM", size = 13, color = C.muted)
        } else {
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(C.accent),
                contentAlignment = Alignment.Center,
            ) {
                Label("S", size = 17, color = Color.White, display = true)
            }
            Title(
                when (a.screen) {
                    Screen.QUICK -> "Quick Start"
                    Screen.PROGRAM -> "Program"
                    Screen.BUILDER -> "Program Builder"
                    Screen.SETTINGS -> "Settings"
                    Screen.HISTORY -> "History"
                    Screen.SUMMARY -> "Complete"
                    else -> "PBM"
                },
                modifier = Modifier.weight(1f),
                size = 19,
            )
        }
        if (a.screen == Screen.HOME) Spacer(Modifier.weight(1f))
        if (a.screen != Screen.SETTINGS)
            Box(
                Modifier.size(36.dp)
                    .clip(CircleShape)
                    .background(C.surface)
                    .clickable { a.navigate(Screen.SETTINGS) }
                    .semantics { contentDescription = "ตั้งค่า" },
                contentAlignment = Alignment.Center,
            ) {
                LineIcon("settings", Modifier.size(19.dp), C.ink)
            }
    }
}

@Composable
fun ScreenColumn(key: Any, content: @Composable ColumnScope.() -> Unit) {
    key(key) {
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 2.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun LoginScreen(a: AppState, onGoogle: () -> Unit) {
    if (a.mailEmail.isNotBlank()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(26.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Wordmark(29)
            Spacer(Modifier.height(20.dp))
            Label("✓", size = 40, color = C.ok)
            Title("เช็คอีเมล", size = 26)
            Label("เราส่งลิงก์ไปที่ ${a.mailEmail} แล้ว")
            Note(
                (if (a.mailKind == "confirm") "เปิดอีเมล แล้วกดลิงก์ยืนยัน จากนั้นกลับมาเข้าสู่ระบบ"
                else "เปิดอีเมล แล้วกดลิงก์ เพื่อตั้งรหัสผ่านใหม่") +
                    " · ถ้าไม่เห็น ลองดูในกล่องจดหมายขยะ (Spam)"
            )
            Action("กลับไปหน้าเข้าสู่ระบบ", primary = false) {
                a.mailEmail = ""
                a.error = ""
            }
        }
        return
    }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    var signup by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Wordmark(29)
        Title(if (signup) "สร้างบัญชีใหม่" else "ยินดีต้อนรับ", size = 24)
        Note(
            if (signup) "ใช้อีเมลของผู้ปกครองหรือของตัวเอง"
            else "เข้าสู่ระบบเพื่อเก็บข้อมูลไว้อย่างปลอดภัย ใช้ได้หลายเครื่อง"
        )
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(C.surface)
                .border(1.dp, C.line, RoundedCornerShape(18.dp))
                .clickable(enabled = !a.busy, onClick = onGoogle)
                .padding(15.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Image(painterResource(R.drawable.ic_google), "Google", Modifier.size(20.dp))
                Label(if (signup) "สมัครด้วย Google" else "เข้าสู่ระบบด้วย Google", size = 15)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HorizontalDivider(Modifier.weight(1f), color = C.line)
            Note("หรือใช้อีเมล")
            HorizontalDivider(Modifier.weight(1f), color = C.line)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Note("อีเมล")
            InputField(email, { email = it }, "", KeyboardType.Email)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Note("รหัสผ่าน" + if (signup) " (อย่างน้อย 6 ตัว)" else "")
            BasicTextField(
                password,
                { password = it },
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.surface)
                    .border(1.dp, C.line, RoundedCornerShape(12.dp)),
                singleLine = true,
                visualTransformation =
                    if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                decorationBox = { field ->
                    Row(
                        Modifier.padding(start = 12.dp, end = 4.dp).heightIn(min = 44.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.weight(1f).padding(vertical = 10.dp)) { field() }
                        Box(
                            Modifier.size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { reveal = !reveal }
                                .semantics {
                                    contentDescription =
                                        if (reveal) "ซ่อนรหัสผ่าน" else "แสดงรหัสผ่าน"
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            LineIcon(if (reveal) "eyeoff" else "eye", Modifier.size(18.dp), C.muted)
                        }
                    }
                },
                textStyle =
                    androidx.compose.ui.text.TextStyle(
                        fontFamily = BodyFont,
                        fontSize = 14.sp,
                        lineHeight = 21.7.sp,
                        color = C.ink,
                    ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(C.accent),
            )
        }
        if (a.error.isNotBlank()) Label(a.error, size = 12, color = C.accent)
        Action(
            if (a.busy) "กำลังส่ง…" else if (signup) "สมัครสมาชิก" else "เข้าสู่ระบบ",
            enabled = !a.busy,
        ) {
            a.signIn(email, password, signup)
        }
        if (!signup)
            TextButton(
                onClick = { a.recover(email) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                enabled = !a.busy,
            ) {
                Note("ลืมรหัสผ่าน?")
            }
        Row(
            Modifier.align(Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Note(if (signup) "มีบัญชีแล้ว?" else "ยังไม่มีบัญชี?")
            TextButton(
                onClick = {
                    signup = !signup
                    a.error = ""
                }
            ) {
                Label(if (signup) "เข้าสู่ระบบ" else "สมัครสมาชิก", size = 13, color = C.accent)
            }
        }
        Action("ใช้แบบไม่ล็อกอิน (เก็บในเครื่องนี้)", primary = false, enabled = !a.busy) {
            a.guest()
        }
    }
}

@Composable
fun InputField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    BasicTextField(
        value,
        onChange,
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(C.surface)
            .border(1.dp, C.line, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        textStyle =
            androidx.compose.ui.text.TextStyle(
                fontFamily = BodyFont,
                fontSize = 14.sp,
                lineHeight = 21.7.sp,
                color = C.ink,
            ),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(C.accent),
        decorationBox = { field ->
            Box {
                if (value.isEmpty() && label.isNotBlank()) Label(label, size = 14, color = C.muted)
                field()
            }
        },
    )
}

@Composable
fun HomeScreen(a: AppState) {
    Box(Modifier.fillMaxSize()) {
        ScreenColumn("home") {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    Modifier.clip(CircleShape)
                        .background(C.surface)
                        .border(1.dp, C.line, CircleShape)
                        .padding(start = 8.dp, end = 11.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Box(
                        Modifier.width(22.dp)
                            .height(11.dp)
                            .border(1.dp, C.muted, RoundedCornerShape(2.dp))
                            .padding(2.dp)
                    ) {
                        Box(
                            Modifier.fillMaxHeight()
                                .fillMaxWidth(a.battery / 100f)
                                .background(C.muted)
                        )
                    }
                    Label("${a.battery}%", size = 12, color = C.muted, weight = FontWeight.Medium)
                }
                val now = java.time.LocalDate.now()
                val last7 =
                    a.history.filter {
                        java.time.Instant.ofEpochMilli(it.date)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate() >= now.minusDays(6)
                    }
                Row(
                    Modifier.weight(1f)
                        .height(31.dp)
                        .clip(CircleShape)
                        .background(C.surface)
                        .clickable { a.navigate(Screen.HISTORY) }
                        .padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Label("7 วัน", size = 11, color = C.muted)
                    Label(
                        "${(last7.sumOf{it.seconds}/60.0).roundToInt()} min",
                        size = 12,
                        weight = FontWeight.Medium,
                    )
                    Spacer(Modifier.weight(1f))
                    (6 downTo 0).forEach { i ->
                        val d = now.minusDays(i.toLong())
                        val active =
                            last7.any {
                                java.time.Instant.ofEpochMilli(it.date)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDate() == d
                            }
                        Box(
                            Modifier.size(10.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (active) C.accent else C.surface2)
                                .then(
                                    if (i == 0)
                                        Modifier.border(1.dp, C.accent, RoundedCornerShape(3.dp))
                                    else Modifier
                                )
                        )
                    }
                    val activeDays =
                        last7
                            .map {
                                java.time.Instant.ofEpochMilli(it.date)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDate()
                            }
                            .distinct()
                            .size
                    Label("$activeDays/7", size = 11, color = C.muted, mono = true)
                    Label("›", size = 16, color = C.muted)
                }
            }
            SessionCard(a)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Title("Programs", size = 15)
                Spacer(Modifier.weight(1f))
                if (a.pro)
                    TextButton(onClick = { a.newProgram() }) {
                        Label("+ New", size = 13, color = C.accent)
                    }
                Toggle("Pro", a.pro) { a.togglePro() }
            }
            val cats = linkedMapOf("relax" to "Relax", "focus" to "Focus", "learn" to "Learn")
            if (a.customs.isNotEmpty()) cats["custom"] = "My Programs"
            if (a.favorites.isNotEmpty()) cats["fav"] = "★ Favorites"
            if (a.category !in cats) a.category = "relax"
            ChipRow(cats.values.toList(), cats[a.category].orEmpty()) { label ->
                a.category = cats.entries.first { it.value == label }.key
            }
            Column {
                a.programs
                    .filter {
                        if (a.category == "fav") it.id in a.favorites else it.cat == a.category
                    }
                    .forEach { p -> ProgramRow(a, p) }
            }
            // The reference links this card to Evaluate. Evaluate is excluded by the requested
            // scope.
            Row(
                Modifier.fillMaxWidth()
                    .border(1.dp, C.line, RoundedCornerShape(18.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.size(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(C.accent.copy(alpha = .1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    LineIcon("pulse", Modifier.size(16.dp))
                }
                Column {
                    Label("Alpha ของคุณ", size = 13)
                    Note("ยังไม่มี · ใช้ความถี่เริ่มต้นของโปรแกรม")
                }
            }
            Spacer(Modifier.height(80.dp))
        }
        Row(
            Modifier.align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
                .clip(CircleShape)
                .background(C.accent)
                .clickable { a.navigate(Screen.QUICK) }
                .padding(start = 14.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.size(26.dp).clip(CircleShape).background(Color.White.copy(alpha = .2f)),
                contentAlignment = Alignment.Center,
            ) {
                Label("+", size = 21, color = Color.White)
            }
            Label("Quick Start", size = 15, color = Color.White, weight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ProgramRow(a: AppState, p: Program) {
    var info by remember(p.id) { mutableStateOf(false) }
    val color =
        Color(
            when (p.cat) {
                "focus" -> 0xFFD89A1E
                "learn" -> 0xFFB5487A
                "custom" -> 0xFF3D6FB4
                else -> 0xFFD9533F
            }
        )
    Column(Modifier.fillMaxWidth().clickable { a.open(p) }) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = .1f)),
                contentAlignment = Alignment.Center,
            ) {
                LineIcon(p.icon, color = color)
            }
            Column(Modifier.weight(1f)) {
                Label(
                    p.name + if (p.id in a.favorites) " ★" else "",
                    size = 15,
                    weight = FontWeight.Medium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Note("${p.frequencyText} · ${p.minutes} min")
                    if (p.alpha)
                        Label(
                            "  α",
                            size = 11,
                            color = Color(0xFF5FB8A8),
                            weight = FontWeight.SemiBold,
                        )
                }
                if (info) {
                    Note(p.goal)
                    a.repo.info.optJSONObject(p.id)?.let { details ->
                        val evidence = a.repo.evidence.getJSONArray(details.getString("ev"))
                        Label(
                            evidence.getString(0),
                            size = 10,
                            color = C.accent,
                            modifier =
                                Modifier.padding(top = 3.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(C.accent.copy(alpha = .08f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                }
            }
            Box(
                Modifier.size(26.dp)
                    .clip(CircleShape)
                    .border(1.dp, C.line, CircleShape)
                    .clickable { info = !info }
                    .semantics { contentDescription = "คำอธิบาย ${p.name}" },
                contentAlignment = Alignment.Center,
            ) {
                Label("i", size = 15, color = C.muted)
            }
            RoundAction(
                if (a.queue.any { it.pid == p.id }) "✓" else "+",
                "ต่อในเซสชัน ${p.name}",
                color = C.accent,
                filled = a.queue.any { it.pid == p.id },
            ) {
                a.toggleQueue(p)
            }
        }
        HorizontalDivider(color = C.line, thickness = 1.dp)
    }
}

@Composable
private fun SessionCard(a: AppState) {
    val q = a.queue
    CardBox(accent = q.isNotEmpty()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Title("Today’s Session", size = 17)
            Spacer(Modifier.weight(1f))
            Note(
                if (q.isEmpty()) "ว่าง"
                else "${q.sumOf{it.minutes}} min · ${q.size} program${if(q.size>1)"s"else""}"
            )
        }
        if (q.isEmpty())
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(C.accent.copy(alpha = .1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Label("+", size = 18, color = C.accent)
                }
                Note("แตะ + ที่โปรแกรมด้านล่างเพื่อเพิ่มเข้าเซสชัน")
            }
        else {
            Timeline(q.flatMap { a.find(it.pid)?.scaled(it.minutes)?.steps ?: emptyList() })
            q.forEachIndexed { i, item ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Label("${i+1}", size = 11, color = C.muted)
                    Label(a.find(item.pid)?.name.orEmpty(), Modifier.weight(1f), size = 12)
                    Stepper(item.minutes.toDouble(), "", 1.0, 60.0) {
                        a.changeQueue(i, it.toInt() - item.minutes)
                    }
                    Box(
                        Modifier.size(25.dp)
                            .clickable(enabled = i > 0) { a.moveQueue(i) }
                            .semantics { contentDescription = "เลื่อนขึ้น" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Note("↑")
                    }
                    Box(
                        Modifier.size(25.dp)
                            .clickable {
                                a.queue.removeAt(i)
                                a.save()
                            }
                            .semantics { contentDescription = "เอาออก" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Note("✕")
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Note("Intensity")
                    Intensity(a)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Note("Closed-loop")
                    Toggle(if (a.closedLoop) "On" else "Off", a.closedLoop) {
                        a.requestClosedLoop()
                    }
                }
            }
            val plan = q.mapNotNull { a.find(it.pid)?.scaled(it.minutes) }
            Action("Start Session   ${q.sumOf { it.minutes }} min") { a.start(plan, true) }
        }
        val recent = a.recentSessions()
        if (recent.isNotEmpty())
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Note("↺ Repeat")
                recent.forEach { r ->
                    Row(
                        Modifier.clip(CircleShape)
                            .background(C.surface2)
                            .border(1.dp, C.line, CircleShape)
                            .clickable { a.repeatSession(r) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Label(r.items.joinToString(" + ") { it.name }, size = 12.5)
                        Label("${r.items.sumOf{it.minutes}} min", size = 11, color = C.accent)
                    }
                }
            }
    }
}

@Composable
fun Intensity(a: AppState) {
    if (a.pro)
        Stepper(a.level.toDouble(), "Level", 1.0, 10.0) {
            a.level = it.toInt()
            a.save()
        }
    else Label("Level 1 · แนะนำ", size = 12, color = C.ok)
}
