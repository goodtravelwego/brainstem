@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.brainstem.stimulate

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlin.math.*

@Composable
fun QuickScreen(a: AppState) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            ScreenColumn("quick") {
                val s = a.quick
                CardBox(padding = 14) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Label("${s.pts.size}", size = 16, weight = FontWeight.SemiBold)
                        Note("/19 points")
                        Spacer(Modifier.weight(1f))
                        Pill("All") {
                            a.quick = s.copy(pts = POINTS)
                            a.save()
                        }
                        Spacer(Modifier.width(6.dp))
                        Pill("Clear") {
                            a.quick = s.copy(pts = emptyList())
                            a.save()
                        }
                    }
                    HeadMap(s.pts, Modifier.align(Alignment.CenterHorizontally).width(240.dp)) { n
                        ->
                        a.quick =
                            s.copy(
                                pts = POINTS.filter { if (it == n) it !in s.pts else it in s.pts }
                            )
                        a.save()
                    }
                    Note("แตะจุดเพื่อเลือก / เอาออก", Modifier.align(Alignment.CenterHorizontally))
                    HorizontalDivider(color = C.line)
                    Expandable("Region guide", REGIONS.map { Color(it.color) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            REGIONS.forEach { r ->
                                GuideRow(
                                    r.name,
                                    r.points.joinToString(" "),
                                    r.description,
                                    Color(r.color),
                                    s.pts.toSet() == r.points.toSet(),
                                    "${r.points.count{it in s.pts}}/${r.points.size}",
                                ) {
                                    a.quick = s.copy(pts = r.points)
                                    a.save()
                                }
                            }
                            Note(
                                "แตะเพื่อเลือกจุดของบริเวณนั้น · General associations · not a medical claim"
                            )
                        }
                    }
                }
                CardBox(padding = 14) {
                    FrequencyEditor(
                        s.copy(dur = a.quickMinutes * 60),
                        {
                            a.quick = it
                            a.save()
                        },
                    )
                }
                CardBox {
                    OptionRow("Duration") {
                        Stepper(a.quickMinutes.toDouble(), "min", 1.0, 60.0) {
                            a.quickMinutes = it.toInt()
                            a.save()
                        }
                    }
                    if (a.pro)
                        OptionRow("Duty cycle") {
                            Stepper(
                                (s.duty.takeIf { it > 0 } ?: a.spec.duty).toDouble(),
                                "%",
                                10.0,
                                90.0,
                                10.0,
                            ) {
                                a.quick = s.copy(duty = it.toInt())
                                a.save()
                            }
                        }
                    OptionRow("Intensity") { Intensity(a) }
                    OptionRow("EEG Closed-loop") {
                        Toggle(if (a.closedLoop) "On" else "Off", a.closedLoop) {
                            a.requestClosedLoop()
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth()
                .background(C.bg)
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Action(
                "Save",
                Modifier.width(73.dp).height(56.dp),
                primary = false,
                enabled = a.quick.pts.isNotEmpty(),
            ) {
                a.saveQuick()
            }
            val min =
                (makePlan(listOf(a.quickProgram()), a.actualLevel, a.closedLoop, false).seconds /
                        60.0)
                    .roundToInt()
            Action(
                if (a.quick.pts.isEmpty()) "เลือกจุดก่อน"
                else "Start  ${a.quick.frequencyText} · $min min",
                Modifier.weight(1f).height(56.dp),
                enabled = a.quick.pts.isNotEmpty(),
            ) {
                a.start(listOf(a.quickProgram()))
            }
        }
    }
}

@Composable
fun FrequencyEditor(s: Step, onChange: (Step) -> Unit, compact: Boolean = false) {
    Segments(listOf("Fixed", "Sweep"), if (s.mode == "sweep") "Sweep" else "Fixed") {
        onChange(s.copy(mode = it.lowercase()))
    }
    if (s.mode != "sweep") {
        if (!compact) {
            var slider by remember(s.f) { mutableFloatStateOf((log10(s.f) / 2 * 1000).toFloat()) }
            val raw = 10.0.pow(slider / 1000.0 * 2)
            val shown = if (raw < 10) (raw * 2).roundToInt() / 2.0 else raw.roundToInt().toDouble()
            Row(
                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RoundAction("−", "ลดความถี่", size = 46, enabled = s.f > 1) {
                    onChange(s.copy(f = (s.f - if (s.f < 10) .5 else 1.0).coerceIn(1.0, 100.0)))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        num(shown),
                        fontSize = 58.sp,
                        lineHeight = 58.sp,
                        fontFamily = DisplayFont,
                        fontWeight = FontWeight.Medium,
                        color = C.ink,
                    )
                    Note("Hz · ${bandName(shown)}")
                }
                RoundAction("+", "เพิ่มความถี่", size = 46, enabled = s.f < 100) {
                    onChange(s.copy(f = (s.f + if (s.f < 10) .5 else 1.0).coerceIn(1.0, 100.0)))
                }
            }
            Slider(
                slider,
                { slider = it },
                valueRange = 0f..1000f,
                onValueChangeFinished = {
                    val f = 10.0.pow(slider / 1000.0 * 2)
                    onChange(
                        s.copy(
                            f =
                                if (f < 10) (f * 2).roundToInt() / 2.0
                                else f.roundToInt().toDouble()
                        )
                    )
                },
                modifier =
                    Modifier.fillMaxWidth().height(26.dp).semantics {
                        contentDescription = "ความถี่"
                    },
                thumb = {
                    Box(
                        Modifier.size(26.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(3.dp, C.accent, CircleShape)
                    )
                },
                track = {
                    Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))) {
                        listOf(
                                .30f to 0xFF9AA5A1,
                                .15f to 0xFF7A5BC9,
                                .09f to 0xFF2A8C82,
                                .05f to 0xFF4FA36B,
                                .15f to 0xFFD89A1E,
                                .26f to 0xFFC2563A,
                            )
                            .forEach { (weight, color) ->
                                Box(
                                    Modifier.weight(weight).fillMaxHeight().background(Color(color))
                                )
                            }
                    }
                },
            )
            FrequencyScale()
        } else
            OptionRow("Frequency") {
                Stepper(s.f, "Hz", 1.0, 100.0, if (s.f < 10) .5 else 1.0) {
                    onChange(s.copy(f = it))
                }
            }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BAND_PRESETS.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { (name, f) ->
                        BandButton(
                            name,
                            num(f),
                            s.f == f,
                            Color(bandColor(f)),
                            Modifier.weight(1f),
                        ) {
                            onChange(s.copy(f = f))
                        }
                    }
                }
            }
        }
    } else {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Note("From")
                Stepper(
                    s.f1,
                    "Hz",
                    1.0,
                    if (compact) 100.0 else min(99.0, s.f2 - 1),
                    narrow = true,
                ) {
                    onChange(s.copy(f1 = it))
                }
            }
            Label("→", size = 22, color = C.muted)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Note("To")
                Stepper(
                    s.f2,
                    "Hz",
                    if (compact) 1.0 else max(2.0, s.f1 + 1),
                    100.0,
                    narrow = true,
                ) {
                    onChange(s.copy(f2 = it))
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SWEEP_PRESETS.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { (n, l, h) ->
                        BandButton(
                            n,
                            "${num(l)}–${num(h)}",
                            s.f1 == l && s.f2 == h,
                            Color(bandColor(l)),
                            Modifier.weight(1f),
                        ) {
                            onChange(s.copy(f1 = l, f2 = h))
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        val width = abs(s.f2 - s.f1)
        val kind =
            when {
                width >= 20 -> "Broad Sweep"
                width == 11.0 -> "Long Sweep"
                width <= 5 -> "Short Sweep"
                else -> "Sweep"
            }
        Note("$kind · ขยับทีละ 1 Hz · ค้างขั้นละ ${"%.1f".format(s.dur/(width+1))} วินาที")
    }
    if (!compact)
        Expandable("Band guide", BANDS.asReversed().map { Color(it.color) }) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val lo = if (s.mode == "sweep") min(s.f1, s.f2) else s.f
                val hi = if (s.mode == "sweep") max(s.f1, s.f2) else s.f
                BANDS.forEach { b ->
                    GuideRow(
                        b.name,
                        "${num(b.lo)}–${num(b.hi)} Hz",
                        b.description,
                        Color(b.color),
                        !(hi < b.lo || lo > b.hi),
                    ) {}
                }
                Note("General associations · not a medical claim")
            }
        }
}

@Composable
private fun FrequencyScale() {
    BoxWithConstraints(Modifier.fillMaxWidth().height(16.dp)) {
        Label("1", Modifier.align(Alignment.CenterStart), size = 10, color = C.muted, mono = true)
        listOf("4" to .30f, "8" to .45f, "13" to .56f, "30" to .74f).forEach { (label, fraction) ->
            Box(
                Modifier.offset(x = maxWidth * fraction - 20.dp).width(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Label(label, size = 10, color = C.muted, mono = true)
            }
        }
        Label(
            "100 Hz",
            Modifier.align(Alignment.CenterEnd),
            size = 10,
            color = C.muted,
            mono = true,
        )
    }
}

@Composable
private fun BandButton(
    name: String,
    value: String,
    selected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) color.copy(alpha = .18f).compositeOver(C.surface) else C.surface2
            )
            .border(1.dp, if (selected) color else C.line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Label(name, size = 12.5)
        Spacer(Modifier.weight(1f))
        Label(value, size = 11, color = if (selected) C.ink else C.muted, mono = true)
    }
}

@Composable
private fun GuideRow(
    name: String,
    subtitle: String,
    description: String,
    color: Color,
    selected: Boolean,
    right: String = "",
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) color.copy(alpha = .1f) else C.surface2)
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.padding(top = 4.dp)
                .width(4.dp)
                .height(34.dp)
                .clip(CircleShape)
                .background(color)
        )
        Column(Modifier.weight(1f)) {
            Label(name, size = 12, weight = FontWeight.Medium)
            Label(subtitle, size = 10, color = C.muted, mono = true)
            Label(description, size = 11, color = C.muted)
        }
        if (right.isNotEmpty()) Label(right, size = 10, color = C.muted)
    }
}

@Composable
fun ProgramScreen(a: AppState) {
    val p = a.selected ?: return
    val scaled = p.scaled(a.programMinutes)
    val index = a.selectedStep.coerceIn(0, scaled.steps.lastIndex)
    val step = scaled.steps[index]
    var deleting by remember { mutableStateOf(false) }
    ScreenColumn(p.id) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Title(p.name, Modifier.weight(1f), size = 23)
            Box(
                Modifier.size(36.dp)
                    .clip(CircleShape)
                    .clickable { a.toggleFavorite(p.id) }
                    .semantics { contentDescription = "ติดดาว" },
                contentAlignment = Alignment.Center,
            ) {
                LineIcon("star", color = if (p.id in a.favorites) Color(0xFFE09A12) else C.line)
            }
        }
        Note("${p.frequencyText} · ${p.goal}")
        if (p.alpha)
            Label("α  ย่านอัลฟาจะปรับตาม IAPF เมื่อวัดใน Evaluate แล้ว", size = 11, color = C.muted)
        HeadMap(step.pts, Modifier.align(Alignment.CenterHorizontally).width(210.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Label(step.label, size = 14, weight = FontWeight.Medium)
            Note(
                "${step.pts.size} จุด · ${step.frequencyText} · ${timeText(step.dur.toDouble())}" +
                    if (a.pro) " · duty ${step.duty.takeIf{it>0}?:a.spec.duty}%" else ""
            )
        }
        Timeline(scaled.steps, index)
        if (scaled.steps.size > 1)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                scaled.steps.forEachIndexed { i, s ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (i == index) C.surface else Color.Transparent)
                            .border(
                                1.dp,
                                if (i == index) C.line else Color.Transparent,
                                RoundedCornerShape(12.dp),
                            )
                            .clickable { a.selectedStep = i }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Color(bandColor(s.f))))
                        Label(s.label, Modifier.weight(1f), size = 12)
                        Label(s.frequencyText, size = 10, color = C.muted, mono = true)
                        Label(timeText(s.dur.toDouble()), size = 10, mono = true)
                    }
                }
            }
        CardBox(padding = 14, radius = 18, gap = 10) {
            OptionRow("Duration") {
                Stepper(a.programMinutes.toDouble(), "min", 1.0, 60.0) {
                    a.programMinutes = it.toInt()
                }
            }
            OptionRow("Intensity") { Intensity(a) }
            OptionRow("EEG Closed-loop") {
                Toggle(if (a.closedLoop) "On" else "Off", a.closedLoop) { a.requestClosedLoop() }
            }
            Note(
                (if (a.programMinutes != p.minutes) "ปกติ ${p.minutes} นาที · " else "") +
                    "Level ${a.actualLevel} · ${a.actualLevel*10}%" +
                    (if (a.actualLevel == 1) " (แนะนำ)" else " · แนะนำ Level 1") +
                    if (a.closedLoop) " · Closed-loop: วัด EEG ระหว่างพัก · ลดแสงเองถ้า high-β สูง"
                    else ""
            )
        }
        if (p.note.isNotBlank()) Note(p.note)
        AboutProgram(a, p)
        if (a.pro)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (p.src == "custom") Pill("Edit") { a.editProgram(false) }
                Pill(if (p.src == "custom") "Duplicate" else "Duplicate & Edit") {
                    a.editProgram(true)
                }
                if (p.src == "custom") Pill("Delete") { deleting = true }
            }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Action("+ Session", Modifier.weight(1f), primary = false) {
                a.queue.add(QueueItem(p.id, a.programMinutes))
                a.save()
                a.navigate(Screen.HOME)
            }
            val min =
                (makePlan(listOf(scaled), a.actualLevel, a.closedLoop, false).seconds / 60.0)
                    .roundToInt()
            Action("Start  $min min", Modifier.weight(1.4f)) { a.start(listOf(scaled)) }
        }
    }
    if (deleting)
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Title("Delete Program") },
            text = { Label(p.name) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleting = false
                        a.deleteProgram()
                    }
                ) {
                    Label("Delete", color = C.accent)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Label("Cancel") } },
        )
}

@Composable
private fun AboutProgram(a: AppState, p: Program) {
    val info = a.repo.info.optJSONObject(p.id) ?: return
    val evidence = a.repo.evidence.getJSONArray(info.getString("ev"))
    CardBox(padding = 14) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Title("About", size = 15)
            Spacer(Modifier.weight(1f))
            Label(
                evidence.getString(0),
                Modifier.clip(CircleShape)
                    .background(C.ok.copy(alpha = .1f))
                    .padding(horizontal = 9.dp, vertical = 3.dp),
                size = 10,
                color = C.ok,
            )
        }
        Label(info.getString("why"), size = 13)
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(C.surface2)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Label("ความถี่   ${info.getString("f")}", size = 12)
            Label(
                "ตำแหน่ง   ${p.steps.map{areaName(it.pts)}.distinct().joinToString(" → ")}",
                size = 12,
            )
            Label("กลไก   NIR → ไมโทคอนเดรีย (CCO) → ATP · การกะพริบ → คลื่นสมอง", size = 12)
        }
        Note(evidence.getString(1))
        val refs = info.getJSONArray("r").strings() + "hashmi"
        Expandable("อ้างอิง ${refs.size}") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                refs.forEachIndexed { i, k ->
                    Label("${i+1}. ${a.repo.references.optString(k)}", size = 11, color = C.muted)
                }
            }
        }
        HorizontalDivider(color = C.line)
        Label(
            "ใช้เพื่อเสริมสุขภาวะ ไม่ใช่การรักษาโรค ถ้ามีภาวะทางการแพทย์หรือเป็นโรคลมชัก ควรปรึกษาแพทย์ก่อน",
            size = 11,
            color = C.muted,
        )
    }
}

@Composable
fun BuilderScreen(a: AppState) {
    val p = a.draft ?: return
    ScreenColumn("builder") {
        InputField(p.name, { a.draft = p.copy(name = it.take(40)) }, "ชื่อโปรแกรม")
        Note(
            "${p.steps.size} step${if(p.steps.size>1)"s"else""} · total ${timeText(p.seconds.toDouble())}"
        )
        Timeline(p.steps, a.editStep)
        p.steps.forEachIndexed { i, s ->
            if (i != a.editStep)
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(C.surface)
                        .clickable { a.editStep = i }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Note("${i+1}")
                    Column(Modifier.weight(1f)) {
                        Label(s.autoLabel(), size = 13, weight = FontWeight.Medium)
                        Note("${timeText(s.dur.toDouble())} · ${if(s.power<=50)"Gentle"else"Full"}")
                    }
                    Note("›")
                }
            else
                CardBox(
                    modifier = Modifier.border(1.5.dp, C.accent, RoundedCornerShape(24.dp)),
                    padding = 14,
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Label(
                            "${i+1}  ${s.autoLabel()}",
                            Modifier.weight(1f),
                            size = 13,
                            weight = FontWeight.Medium,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("↑") { a.builderAction("up") }
                        Pill("↓") { a.builderAction("down") }
                        Pill("Duplicate") { a.builderAction("duplicate") }
                        Pill("Delete") { a.builderAction("delete") }
                    }
                    Note("Area")
                    var custom by
                        remember(i) {
                            mutableStateOf(AREAS.values.none { it.toSet() == s.pts.toSet() })
                        }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AREAS.forEach { (n, pts) ->
                            Pill(n, s.pts.toSet() == pts.toSet()) {
                                custom = false
                                a.updateStep(s.copy(pts = pts))
                            }
                        }
                        Pill("Custom…", custom) { custom = !custom }
                    }
                    if (custom) {
                        HeadMap(
                            s.pts,
                            Modifier.align(Alignment.CenterHorizontally).width(220.dp),
                        ) { n ->
                            a.updateStep(
                                s.copy(
                                    pts =
                                        POINTS.filter { if (it == n) it !in s.pts else it in s.pts }
                                )
                            )
                        }
                        Note("แตะจุดเพื่อเลือก / เอาออก")
                    }
                    Note("Frequency")
                    FrequencyEditor(s, a::updateStep, compact = true)
                    OptionRow("Duration") {
                        Stepper((s.dur / 60.0).roundToInt().toDouble(), "min", 1.0, 30.0) {
                            a.updateStep(s.copy(dur = it.toInt() * 60))
                        }
                    }
                    Note("Power")
                    Segments(listOf("Gentle", "Full"), if (s.power <= 50) "Gentle" else "Full") {
                        a.updateStep(s.copy(power = if (it == "Gentle") 50 else 100))
                    }
                    OptionRow("Duty cycle") {
                        Stepper(
                            (s.duty.takeIf { it > 0 } ?: a.spec.duty).toDouble(),
                            "%",
                            10.0,
                            90.0,
                            10.0,
                        ) {
                            a.updateStep(s.copy(duty = it.toInt()))
                        }
                    }
                }
        }
        Action("+ Add Step", primary = false) { a.builderAction("add") }
        if (a.error.isNotBlank()) Label(a.error, size = 12, color = C.accent)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Action("Cancel", Modifier.weight(1f), primary = false) { a.back() }
            Action("Save Program", Modifier.weight(2f)) { a.saveDraft() }
        }
    }
}
