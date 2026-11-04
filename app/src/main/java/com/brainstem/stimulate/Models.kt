package com.brainstem.stimulate

import kotlin.math.*
import kotlin.random.Random

val POINTS =
    listOf(
        "Fp1",
        "Fp2",
        "F7",
        "F3",
        "Fz",
        "F4",
        "F8",
        "T3",
        "C3",
        "Cz",
        "C4",
        "T4",
        "T5",
        "P3",
        "Pz",
        "P4",
        "T6",
        "O1",
        "O2",
    )
val POINT_XY =
    listOf(
        90f to 32f,
        130f to 32f,
        52f to 60f,
        81f to 67f,
        110f to 65f,
        139f to 67f,
        168f to 60f,
        33f to 106f,
        71f to 106f,
        110f to 106f,
        149f to 106f,
        187f to 106f,
        52f to 152f,
        81f to 145f,
        110f to 147f,
        139f to 145f,
        168f to 152f,
        90f to 180f,
        130f to 180f,
    )

data class Region(
    val name: String,
    val color: Long,
    val points: List<String>,
    val description: String,
)

val REGIONS =
    listOf(
        Region(
            "Frontal",
            0xFF8A5BC0,
            listOf("Fp1", "Fp2", "F7", "F3", "Fz", "F4", "F8"),
            "Executive function, decision making, inhibition, working memory",
        ),
        Region(
            "Midline",
            0xFF5E86C9,
            listOf("Fz", "Cz", "Pz"),
            "Cognitive flexibility, processing speed, complex attention",
        ),
        Region(
            "Central",
            0xFF3F7FD0,
            listOf("C3", "Cz", "C4"),
            "Planning and control of balance and voluntary movement",
        ),
        Region(
            "Temporal",
            0xFF6FA85A,
            listOf("T3", "T4", "T5", "T6"),
            "Auditory processing, memory, speech, language, emotion",
        ),
        Region(
            "Parietal",
            0xFFE8833A,
            listOf("P3", "Pz", "P4"),
            "Sensory integration, vigilance, calculation, self-awareness",
        ),
        Region(
            "Limbic",
            0xFFE0708A,
            listOf("Fp1", "Fp2", "O1", "O2"),
            "Autonomic regulation, motivation, social-emotional processing",
        ),
        Region(
            "Occipital",
            0xFFD2455B,
            listOf("O1", "O2"),
            "Visual processing, spatial orientation, memory encoding",
        ),
    )

data class Band(
    val name: String,
    val lo: Double,
    val hi: Double,
    val color: Long,
    val description: String,
)

val BANDS =
    listOf(
        Band("HiGamma", 55.0, 85.0, 0xFF8E3B8F, "High-level processing, binding"),
        Band("Gamma", 35.0, 55.0, 0xFF7A3FA0, "Complex thinking, integration, networking"),
        Band("HiBeta", 25.0, 35.0, 0xFF5B7FD1, "Alert, high arousal"),
        Band("MidBeta", 15.0, 25.0, 0xFF4FA36B, "Engaged, externally oriented tasks"),
        Band("LoBeta · SMR", 12.0, 15.0, 0xFF6FAE3A, "Calm focus, inhibition"),
        Band("Alpha", 8.0, 12.0, 0xFFE0A100, "Aware, relaxed, present"),
        Band("Thalpha", 6.0, 9.0, 0xFFE9846A, "Creative, inward attention"),
        Band("Theta", 4.0, 7.0, 0xFFE05A7A, "Internally focused, day-dreaming"),
        Band("Delta", 1.0, 4.0, 0xFFB33A5C, "Deep rest, recovery, sleep"),
    )
val BAND_PRESETS =
    listOf(
        "Delta" to 2.0,
        "Theta" to 6.0,
        "Alpha" to 10.0,
        "SMR" to 14.0,
        "Beta" to 18.0,
        "Gamma" to 40.0,
    )
val SWEEP_PRESETS =
    listOf(
        Triple("Alpha", 8.0, 12.0),
        Triple("LoBeta", 12.0, 15.0),
        Triple("Beta", 24.0, 35.0),
        Triple("Gamma", 40.0, 51.0),
        Triple("Broad", 1.0, 45.0),
    )
val AREAS =
    linkedMapOf(
        "Whole head" to POINTS,
        "Front" to REGIONS[0].points,
        "Back" to listOf("T5", "P3", "Pz", "P4", "T6", "O1", "O2"),
        "Limbic" to listOf("Fp1", "Fp2", "O1", "O2"),
    )

fun areaName(pts: List<String>) =
    AREAS.entries.firstOrNull { it.value.toSet() == pts.toSet() }?.key ?: "Custom · ${pts.size} จุด"

fun num(n: Double) =
    if (n == n.toInt().toDouble()) n.toInt().toString() else "%.1f".format(java.util.Locale.US, n)

fun timeText(sec: Double): String {
    val s = ceil(max(0.0, sec)).toInt()
    return "${s/60}:${(s%60).toString().padStart(2,'0')}"
}

fun bandName(f: Double) =
    when {
        f < 4 -> "Delta"
        f < 8 -> "Theta"
        f < 12 -> "Alpha"
        f < 15 -> "SMR"
        f < 30 -> "Beta"
        else -> "Gamma"
    }

fun bandColor(f: Double) =
    when {
        f < 4 -> 0xFF9AA5A1
        f < 8 -> 0xFF7A5BC9
        f < 13 -> 0xFF2A8C82
        f < 30 -> 0xFFD89A1E
        else -> 0xFFC2563A
    }

data class Step(
    val label: String = "Whole head · 10 Hz",
    val pts: List<String> = POINTS,
    val mode: String = "fixed",
    val f: Double = 10.0,
    val f1: Double = 8.0,
    val f2: Double = 12.0,
    val dur: Int = 600,
    val power: Int = 100,
    val duty: Int = 0,
    val phase: String = "",
    val programIndex: Int = 0,
    val group: Int = 0,
) {
    val frequencyText
        get() = if (mode == "sweep") "${num(f1)}→${num(f2)} Hz" else "${num(f)} Hz"

    fun autoLabel() = "${areaName(pts)} · $frequencyText"
}

data class Program(
    val id: String,
    val name: String,
    val cat: String = "custom",
    val icon: String = "sun",
    val goal: String = "สร้างเอง",
    val note: String = "",
    val src: String = "custom",
    val fixedHz: Boolean = false,
    val steps: List<Step> = listOf(Step()),
) {
    val seconds
        get() = steps.sumOf { it.dur }

    val minutes
        get() = (seconds / 60.0).roundToInt()

    val alpha
        get() =
            src != "custom" &&
                src != "clinic" &&
                !fixedHz &&
                steps.any { it.mode == "fixed" && it.f in 8.0..12.5 }

    val frequencyText
        get(): String {
            val f = steps.flatMap { if (it.mode == "sweep") listOf(it.f1, it.f2) else listOf(it.f) }
            return if (f.min() == f.max()) "${num(f.min())} Hz"
            else "${num(f.min())}–${num(f.max())} Hz"
        }

    fun scaled(minutes: Int): Program {
        val scale = minutes * 60.0 / seconds
        return copy(steps = steps.map { it.copy(dur = max(10, (it.dur * scale).roundToInt())) })
    }
}

data class QueueItem(val pid: String, val minutes: Int)

data class Profile(
    val id: String,
    val name: String,
    val age: Int = 9,
    val cloud: String = "",
    val realName: String = name,
    val sex: String = "",
    val birth: String = "",
    val type: String = if (age < 18) "kid" else "adult",
    val hand: String = "",
    val guardianName: String = "",
    val guardianRelation: String = "",
    val guardianPhone: String = "",
    val photo: String = "",
    val photoPath: String = "",
)

data class Spec(
    val wavelength: Int = 810,
    val irradiance: Int = 40,
    val duty: Int = 50,
    val dailyMax: Int = 40,
)

data class SessionItem(val pid: String, val name: String, val minutes: Int, val seconds: Int)

data class SessionRecord(
    val date: Long = System.currentTimeMillis(),
    val complete: Boolean,
    val seconds: Int,
    val level: Int,
    val items: List<SessionItem>,
    val dose: Map<String, Double>,
    val closedLoop: Boolean = false,
    val alphaTrace: List<Double> = emptyList(),
    val deltas: List<Double> = emptyList(),
    val iapf: Double = 0.0,
    val initialLevel: Int = level,
    val events: List<String> = emptyList(),
    val plannedSeconds: Int = 0,
    val profileName: String = "",
    val simulationSpeed: Int = 0,
)

fun weeklyMinutes(
    history: List<SessionRecord>,
    today: java.time.LocalDate = java.time.LocalDate.now(),
): List<Int> =
    (6 downTo 0).map { offset ->
        val day = today.minusDays(offset.toLong())
        (history
                .filter {
                    java.time.Instant.ofEpochMilli(it.date)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate() == day
                }
                .sumOf { it.seconds } / 60.0)
            .roundToInt()
    }

data class RunPlan(
    val programs: List<Program>,
    val steps: List<Step>,
    val level: Int,
    val closedLoop: Boolean,
    val fromQueue: Boolean,
) {
    val seconds
        get() = steps.sumOf { it.dur }
}

fun makePlan(
    programs: List<Program>,
    level: Int,
    closedLoop: Boolean,
    fromQueue: Boolean,
): RunPlan {
    val steps = programs.flatMapIndexed { j, p -> p.steps.map { it.copy(programIndex = j) } }
    if (!closedLoop) return RunPlan(programs, steps, level, false, fromQueue)
    val out =
        mutableListOf(
            Step(
                label = "Baseline EEG",
                pts = emptyList(),
                mode = "none",
                dur = 60,
                phase = "pre",
                programIndex = -1,
            )
        )
    steps.forEachIndexed { i, s ->
        val n = max(1, ceil(s.dur / 180.0).toInt())
        repeat(n) {
            out += s.copy(dur = (s.dur.toDouble() / n).roundToInt(), group = i)
            out +=
                Step(
                    label = "Measure",
                    pts = emptyList(),
                    mode = "none",
                    dur = 45,
                    phase = "meas",
                    programIndex = -1,
                    group = i,
                )
        }
    }
    out[out.lastIndex] = out.last().copy(label = "After EEG", dur = 60, phase = "post")
    return RunPlan(programs, out, level, true, fromQueue)
}

fun frequencyAt(s: Step, t: Double): Double {
    if (s.mode == "none") return 0.0
    if (s.mode != "sweep") return s.f.coerceIn(1.0, 100.0)
    val n = abs(s.f2 - s.f1) + 1
    val k = min(n - 1, floor(t / (s.dur / n)))
    return s.f1 + if (s.f2 >= s.f1) k else -k
}

/** Pure session engine. Simulator only: it never sends commands to a physical device. */
class SessionEngine(val plan: RunPlan, val spec: Spec, val seed: Long = Random.nextLong()) {
    var index = 0
        private set

    var stepElapsed = 0.0
        private set

    var elapsed = 0.0
        private set

    var level = plan.level
        private set

    var paused = false
    var finished = false
        private set

    val dose = POINTS.associateWith { 0.0 }.toMutableMap()
    val programSeconds = DoubleArray(plan.programs.size)
    val eeg = doubleArrayOf(1.0, 1.0, 1.0, 1.0)
    val trace = mutableListOf<Double>()
    val events = mutableListOf<String>()
    private val random = Random(seed)
    val iapf = ((9.3 + random.nextDouble() * 1.2) * 10).roundToInt() / 10.0
    private val best = ((iapf + random.nextDouble() * 1.2 - .5) * 10).roundToInt() / 10.0
    private var spike = 0.0
    private var pending = 0.0
    private var ticks = 0L
    private var stepTicks = 0
    private val samples = mutableListOf<DoubleArray>()
    private var lastMeasure: DoubleArray? = null
    private var base: DoubleArray? = null
    val bands: List<Double>?
        get() =
            samples
                .takeLast(8)
                .takeIf { it.isNotEmpty() }
                ?.let { values ->
                    (0..3).map { i ->
                        (values.map { it[i] }.average() / (base?.get(i) ?: 1.0) - 1) * 100
                    }
                }

    val step
        get() = plan.steps[index.coerceAtMost(plan.steps.lastIndex)]

    val remaining
        get() = max(0.0, plan.seconds - elapsed)

    fun advance(seconds: Double) {
        if (paused || finished || seconds <= 0) return
        // Fixed simulation increments make seeded replay independent of UI frame timing.
        pending += seconds
        while (pending >= .1 - 1e-8 && !finished) {
            val s = step
            val dt = .1
            pending -= dt
            if (s.phase.isEmpty()) {
                val energy =
                    spec.irradiance *
                        (level / 10.0) *
                        (s.power / 100.0) *
                        ((s.duty.takeIf { it > 0 } ?: spec.duty) / 100.0) / 1000.0 * dt
                s.pts.forEach { dose[it] = dose.getValue(it) + energy }
                programSeconds[s.programIndex] += dt
                val f = frequencyAt(s, stepElapsed)
                if (plan.closedLoop) {
                    val d = abs(f - best)
                    val targets =
                        doubleArrayOf(
                            1 +
                                (if (d < 1.8) .32 * (1 - d / 1.8)
                                else if (f >= 7 && f < 13) .06 else 0.0) +
                                (if (f >= 30) .04 else 0.0),
                            1 + (if (f >= 13 && f < 30) .16 else if (f >= 30) .12 else 0.0),
                            1 + (if (f < 8) .2 else if (f >= 13) -.09 else 0.0),
                            1 + (if (f >= 25 && f < 36) .08 else 0.0),
                        )
                    eeg.indices.forEach { eeg[it] += (targets[it] - eeg[it]) * min(1.0, dt / 50) }
                }
            } else {
                eeg.indices.forEach { eeg[it] += (1 - eeg[it]) * min(1.0, dt / 120) * .6 }
                if ((stepTicks + 1) / 10.0 > 3) {
                    if (spike <= 0 && random.nextDouble() < dt * .01)
                        spike = 6 + random.nextDouble() * 8
                    if (spike > 0) spike -= dt
                    samples +=
                        DoubleArray(4) { i ->
                            eeg[i] +
                                (random.nextDouble() - .5) * .05 +
                                if (i == 3 && spike > 0) .45 else 0.0
                        }
                }
            }
            ticks++
            stepTicks++
            stepElapsed = stepTicks / 10.0
            elapsed = ticks / 10.0
            if (stepTicks >= s.dur * 10) {
                if (s.phase.isNotEmpty()) endMeasurement(s)
                if (index == plan.steps.lastIndex) finished = true
                else {
                    index++
                    stepElapsed = 0.0
                    stepTicks = 0
                    samples.clear()
                }
            }
        }
    }

    private fun endMeasurement(s: Step) {
        if (samples.isEmpty()) return
        val m = DoubleArray(4) { i -> samples.map { it[i] }.average() }
        lastMeasure = m
        if (s.phase == "pre") {
            base = m
            events += "ค่าเริ่มต้นพร้อม · ยอดอัลฟา $iapf Hz"
            return
        }
        val b = base ?: return
        val alpha = ((m[0] / b[0] - 1) * 100).roundToInt()
        val hb = ((m[3] / b[3] - 1) * 100).roundToInt()
        trace += alpha.toDouble()
        if (s.phase == "post") return
        if (hb > 35 && level > 1) {
            level--
            events += "high-β สูง +$hb% → ลดแสงเหลือ Level $level"
        }
        val previous = plan.steps.take(index).lastOrNull { it.phase.isEmpty() }
        val focus = previous?.let { plan.programs[it.programIndex].cat == "focus" } == true
        val score = if (focus) ((m[1] / m[2] / (b[1] / b[2]) - 1) * 100).roundToInt() else alpha
        events +=
            "ช่วงที่ผ่านมา ${if (score > 0) "+" else ""}$score% ${if (focus) "(β/θ)" else "(α)"}"
    }

    fun endEarly() {
        if (!finished && step.phase.isNotEmpty()) endMeasurement(step)
        paused = true
    }

    fun record(complete: Boolean) =
        SessionRecord(
            complete = complete,
            seconds = elapsed.roundToInt(),
            level = level,
            items =
                plan.programs.mapIndexed { j, p ->
                    SessionItem(p.id, p.name, p.minutes, programSeconds[j].roundToInt())
                },
            dose = dose.toMap(),
            closedLoop = plan.closedLoop,
            alphaTrace = trace.toList(),
            deltas =
                if (base != null && lastMeasure != null)
                    eeg.indices.map {
                        ((lastMeasure!![it] / base!![it] - 1) * 100).roundToInt().toDouble()
                    }
                else emptyList(),
            iapf = iapf,
            initialLevel = plan.level,
            events = events.takeLast(6),
            plannedSeconds = plan.seconds,
        )
}

/** The reference prototype's impedance simulator. Values are never hardware measurements. */
class HeadsetCheckModel(
    val previouslyChecked: Boolean,
    private val random: Random = Random.Default,
) {
    val channels = listOf("F3", "F4", "Fz", "Cz", "Pz", "Ref", "GND")
    val values =
        channels
            .associateWith {
                if (previouslyChecked) 6 + random.nextDouble() * 6
                else 20 + random.nextDouble() * 25
            }
            .toMutableMap()
    private val targets = channels.associateWith { 3.5 + random.nextDouble() * 5 }.toMutableMap()
    var elapsed = 0.0
        private set

    var hum = if (previouslyChecked) 1.0 else 3.2
        private set

    private var humTarget = if (random.nextDouble() < .12) 2.2 else .8
    private var stable = 0.0

    init {
        if (!previouslyChecked && random.nextDouble() < .35)
            targets[channels[random.nextInt(channels.size)]] = 24.0
    }

    val ready
        get() = values.values.all { it < 10 } && hum < 1.5

    val automatic
        get() = previouslyChecked && ready && stable >= .9

    val fixChannel
        get() = if (elapsed > 1.5) channels.firstOrNull { values.getValue(it) >= 20 } else null

    val needsHumFix
        get() = elapsed > 1.5 && fixChannel == null && hum >= 1.5

    fun advance(dt: Double) {
        val t = dt.coerceIn(0.0, .1)
        elapsed += t
        channels.forEach { n ->
            values[n] =
                values.getValue(n) +
                    (targets.getValue(n) - values.getValue(n)) *
                        min(1.0, t * if (previouslyChecked) 3.0 else 1.4)
        }
        hum += (humTarget - hum) * min(1.0, t * 1.5)
        stable = if (ready) stable + t else 0.0
    }

    fun fix() {
        val n = fixChannel
        if (n != null) targets[n] = 3.5 + random.nextDouble() * 4
        else if (needsHumFix) humTarget = .8
    }
}
