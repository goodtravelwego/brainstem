@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.brainstem.stimulate

import android.os.SystemClock
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlin.math.*
import kotlinx.coroutines.delay

@Composable
fun RunScreen(a: AppState) {
    val r = a.engine ?: return
    val revision = a.revision
    val s = r.step
    val motion = rememberInfiniteTransition(label = "run motion")
    val wavePhase by
        motion.animateFloat(
            0f,
            1f,
            infiniteRepeatable(tween(6000, easing = LinearEasing)),
            label = "wave phase",
        )
    LaunchedEffect(r) {
        var last = SystemClock.elapsedRealtime()
        while (a.engine === r) {
            delay(100)
            val now = SystemClock.elapsedRealtime()
            val dt = (now - last) / 1000.0
            last = now
            a.tick(dt.coerceAtMost(.5) * a.speed)
        }
    }
    val basePalette = C
    val dark = basePalette.bg == Dark.bg
    val p =
        if (dark)
            basePalette.copy(
                ink = Color.White,
                muted = Color.White.copy(alpha = .6f),
                accent = Color(0xFFFF8A7A),
            )
        else basePalette
    // Reuse the visual transition's frame invalidation without a second frame loop.
    // A separate withFrameNanos loop also drives virtual test time while waiting for idle.
    val realSeconds = remember(wavePhase) { SystemClock.elapsedRealtime() / 1000.0 }
    val headSize = minOf(270.dp, LocalConfiguration.current.screenWidthDp.dp * .7f)
    CompositionLocalProvider(LocalPalette provides p) {
        Column(
            Modifier.fillMaxSize().drawBehind {
                val center = Offset(size.width * .5f, size.height * if (dark) .35f else .30f)
                val farthestCorner = hypot(size.width * .5f, max(center.y, size.height - center.y))
                drawRect(
                    Brush.radialGradient(
                        0f to Color(if (dark) 0xFF2A1F2B else 0xFFFFEDE8),
                        (if (dark) .70f else .72f) to Color(if (dark) 0xFF140F16 else 0xFFF6F8F7),
                        1f to Color(if (dark) 0xFF140F16 else 0xFFF6F8F7),
                        center = center,
                        radius = farthestCorner,
                    )
                )
            }
        ) {
            Column(
                Modifier.weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 18.dp, end = 18.dp, top = 52.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Label(
                    if (s.phase.isNotEmpty()) "Closed-loop"
                    else r.plan.programs[s.programIndex].name,
                    modifier = Modifier.fillMaxWidth(),
                    size = 16,
                    display = true,
                    weight = FontWeight.Medium,
                )
                Timeline(r.plan.steps, r.index, runStyle = true)
                if (s.phase.isEmpty()) {
                    val fVisual = min(1.0, .5 + .012 * frequencyAt(s, r.stepElapsed))
                    val w = .5 + .5 * sin(realSeconds * 2 * PI * fVisual)
                    val pulse = if (r.paused) .28 else .35 + .65 * w * w * (3 - 2 * w)
                    val power = (r.level * .1 * s.power / 100.0 * 1.5).coerceIn(.5, 1.0)
                    HeadGlow(
                        s.pts,
                        values = s.pts.associateWith { if (r.paused) pulse else power * pulse },
                        modifier = Modifier.width(headSize).padding(top = 4.dp),
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            timeText(r.remaining),
                            modifier =
                                Modifier.height(46.dp)
                                    .wrapContentHeight(
                                        Alignment.CenterVertically,
                                        unbounded = true,
                                    ),
                            letterSpacing = (-1).sp,
                            fontSize = 46.sp,
                            lineHeight = 46.sp,
                            fontFamily = DisplayFont,
                            fontWeight = FontWeight.Medium,
                            color = p.ink,
                        )
                        Label("remaining", size = 12, color = p.muted)
                    }
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Label(
                            num(frequencyAt(s, r.stepElapsed)),
                            size = 26,
                            color = p.accent,
                            display = true,
                        )
                        Label("Hz", size = 14, color = p.muted)
                    }
                    Canvas(Modifier.width(260.dp).height(26.dp)) {
                        val path = Path()
                        val cycles =
                            (frequencyAt(s, r.stepElapsed) / 2.2).roundToInt().coerceIn(2, 18)
                        for (i in 0..260) {
                            val x = size.width * i / 260
                            val y =
                                size.height / 2 +
                                    -sin((i / 260.0 + wavePhase) * cycles * PI) * 9.dp.toPx()
                            if (i == 0) path.moveTo(x, y.toFloat()) else path.lineTo(x, y.toFloat())
                        }
                        drawPath(
                            path,
                            Brush.horizontalGradient(
                                0f to p.accent.copy(alpha = 0f),
                                .15f to p.accent.copy(alpha = .85f),
                                .85f to p.accent.copy(alpha = .85f),
                                1f to p.accent.copy(alpha = 0f),
                            ),
                            style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                    Label(s.label, size = 15, weight = FontWeight.Medium, align = TextAlign.Center)
                    if (r.plan.closedLoop) Note("Level ${r.level} · EEG ปิดระหว่างกระตุ้น")
                } else {
                    Spacer(Modifier.height(30.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        LineIcon("eyeoff", Modifier.size(18.dp), p.ink)
                        Label("หลับตา", size = 16, weight = FontWeight.Medium)
                    }
                    Note(
                        "${when(s.phase){"pre"->"Baseline"
"post"->"After"
else->"Measure"}} · EEG"
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Note(
                            when (s.phase) {
                                "pre" -> "BASELINE"
                                "post" -> "AFTER"
                                else -> "MEASURE"
                            }
                        )
                        Label("α ${num(r.iapf)} Hz", size = 12, mono = true, color = p.muted)
                    }
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Column(Modifier.fillMaxWidth().aspectRatio(640f / 300f)) {
                            listOf("F3", "F4", "Fz", "Cz", "Pz").forEachIndexed { i, ch ->
                                Row(
                                    Modifier.fillMaxWidth().weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Label(
                                        ch,
                                        Modifier.width(16.dp),
                                        size = 7,
                                        color = p.muted,
                                        mono = true,
                                    )
                                    Canvas(Modifier.weight(1f).fillMaxHeight()) {
                                        val path = Path()
                                        for (j in 0..604) {
                                            val x = size.width * j / 604f
                                            val t = realSeconds + j / 220.0
                                            val phase = i * 1.7
                                            val envelope = .55 + .45 * sin(2 * PI * .25 * t + phase)
                                            val posterior =
                                                if (i == 4) 1.5 else if (i >= 2) 1.25 else .9
                                            val signal =
                                                r.eeg[0] *
                                                    posterior *
                                                    envelope *
                                                    sin(2 * PI * r.iapf * t + phase) +
                                                    .45 *
                                                        r.eeg[2] *
                                                        sin(2 * PI * 5.5 * t + phase * 2) +
                                                    .22 * r.eeg[1] * sin(2 * PI * 19 * t + i)
                                            val y = size.height / 2 - signal * size.height * .26
                                            if (j == 0) path.moveTo(x, y.toFloat())
                                            else path.lineTo(x, y.toFloat())
                                        }
                                        drawPath(
                                            path,
                                            Brush.horizontalGradient(
                                                0f to
                                                    p.accent.copy(alpha = if (i == 4) 1f else .75f),
                                                .78f to
                                                    p.accent.copy(alpha = if (i == 4) 1f else .75f),
                                                1f to p.accent.copy(alpha = 0f),
                                            ),
                                            style =
                                                Stroke(if (i == 4) 1.dp.toPx() else .7.dp.toPx()),
                                        )
                                    }
                                }
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            listOf("θ" to 2, "α" to 0, "β" to 1, "hβ" to 3).forEach { (n, i) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Box(
                                        Modifier.size(7.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Color(
                                                    when (i) {
                                                        2 -> 0xFF8FA6E8
                                                        0 -> 0xFF6CCFB9
                                                        1 -> 0xFFF2C14E
                                                        else -> 0xFFF28B74
                                                    }
                                                )
                                            )
                                    )
                                    Label(
                                        "$n ${if(s.phase=="pre") "—" else r.bands?.get(i)?.roundToInt()?.let{if(it>=0)"+$it%"else"$it%"} ?: "…"}",
                                        size = 13,
                                        mono = true,
                                    )
                                }
                            }
                        }
                    }
                    LinearProgressIndicator(
                        progress = { (r.stepElapsed / s.dur).toFloat() },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = p.accent,
                        trackColor = p.line,
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (dark) Color.White.copy(alpha = .1f) else p.surface)
                        .border(
                            1.dp,
                            if (dark) Color.Transparent else p.line,
                            RoundedCornerShape(18.dp),
                        )
                        .clickable { a.pause() },
                    contentAlignment = Alignment.Center,
                ) {
                    Label(
                        if (r.paused) "Resume" else "Pause",
                        size = 16,
                        weight = FontWeight.Medium,
                    )
                }
                Box(
                    Modifier.weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (dark) Color(0xFFE8735E).copy(alpha = .2f)
                            else Color(0xFFD9533F).copy(alpha = .12f).compositeOver(p.surface)
                        )
                        .clickable { a.finish(false) },
                    contentAlignment = Alignment.Center,
                ) {
                    Label(
                        "Stop",
                        size = 16,
                        color = if (dark) Color(0xFFFFB4A6) else Color(0xFFB23E2C),
                        weight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryScreen(a: AppState) {
    val r = a.summary ?: return
    val context = LocalContext.current
    LaunchedEffect(r.date) {
        if (r.complete) {
            runCatching { completionChime(context) }
        }
    }
    ScreenColumn("summary") {
        Column(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(C.accent.copy(alpha = .16f)),
                contentAlignment = Alignment.Center,
            ) {
                Label("✓", size = 26, color = C.accent)
            }
            Title(if (r.complete) "Session Complete" else "Stopped Early", size = 24)
            Note(r.items.joinToString(" → ") { it.name })
        }
        val used = r.dose.values.filter { it > 0 }
        val avg = if (used.isEmpty()) 0.0 else used.average()
        val peak = r.dose.values.maxOrNull() ?: 0.0
        StatTiles(
            listOf(
                timeText(r.seconds.toDouble()) to "เวลา",
                "L${r.level}" to "ความแรง",
                "%.2f".format(avg) to "J/cm² เฉลี่ย",
            )
        )
        if (r.closedLoop && r.deltas.isNotEmpty())
            CardBox(padding = 14, topPadding = 12, bottomPadding = 12, radius = 18, gap = 8) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Label("EEG · Alpha", weight = FontWeight.Medium)
                    Note("IAPF ${num(r.iapf)} Hz")
                }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "${if(r.deltas[0]>0)"+"else""}${r.deltas[0].roundToInt()}%",
                        fontFamily = DisplayFont,
                        modifier =
                            Modifier.height(34.dp)
                                .wrapContentHeight(Alignment.CenterVertically, unbounded = true),
                        fontSize = 34.sp,
                        lineHeight = 34.sp,
                        color = if (r.deltas[0] >= 10) C.ok else C.ink,
                    )
                    val big = abs(r.deltas[0]) >= 10
                    Label(
                        if (!big) "อยู่ในช่วงปกติ ±10%"
                        else if (r.deltas[0] > 0) "เพิ่มขึ้น" else "ลดลง",
                        Modifier.clip(CircleShape)
                            .background(if (big) C.ok.copy(alpha = .14f) else C.surface2)
                            .padding(horizontal = 9.dp, vertical = 2.dp),
                        size = 11.5,
                        color = if (big) C.ok else C.muted,
                    )
                }
                val p = C
                val measurer = rememberTextMeasurer()
                val axisStyle = TextStyle(fontFamily = BodyFont, fontSize = 10.sp, color = p.muted)
                val beforeLabel = measurer.measure("ก่อน", axisStyle)
                val afterLabel = measurer.measure("หลัง", axisStyle)
                Canvas(Modifier.fillMaxWidth().aspectRatio(300f / 84f)) {
                    val values = listOf(0.0) + r.alphaTrace
                    val maxVal = max(15.0, values.maxOf { abs(it) })
                    val scale = size.width / 300f
                    fun x(i: Int) = (14f + i * 272f / max(1, values.size - 1)) * scale
                    fun y(v: Double) = (42f - v / maxVal * 30f).toFloat() * scale
                    drawLine(
                        p.line,
                        Offset(10 * scale, size.height / 2),
                        Offset(290 * scale, size.height / 2),
                        scale,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3 * scale, 3 * scale)),
                    )
                    drawRect(
                        p.muted.copy(alpha = .08f),
                        Offset(10 * scale, y(10.0)),
                        androidx.compose.ui.geometry.Size(280 * scale, y(-10.0) - y(10.0)),
                    )
                    val path = Path()
                    values.forEachIndexed { i, v ->
                        if (i == 0) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v))
                    }
                    drawPath(path, p.accent, style = Stroke(2 * scale, join = StrokeJoin.Round))
                    values.forEachIndexed { i, v ->
                        drawCircle(
                            p.accent,
                            (if (i == values.lastIndex) 4f else 2.6f) * scale,
                            Offset(x(i), y(v)),
                        )
                    }
                    drawText(
                        beforeLabel,
                        topLeft = Offset(x(0), size.height - 2 * scale - beforeLabel.size.height),
                    )
                    drawText(
                        afterLabel,
                        topLeft =
                            Offset(
                                x(values.lastIndex) - afterLabel.size.width,
                                size.height - 2 * scale - afterLabel.size.height,
                            ),
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf("β" to 1, "θ" to 2, "high-β" to 3).forEach { (n, i) ->
                        Label(
                            "$n ${if(r.deltas[i]>=0)"+"else""}${r.deltas[i].roundToInt()}%",
                            modifier =
                                Modifier.clip(RoundedCornerShape(10.dp))
                                    .background(p.surface2)
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
                            size = 12.5,
                            color = if (i == 3 && r.deltas[i] > 20) Color(0xFF9A6200) else p.ink,
                            mono = true,
                        )
                    }
                }
                if (r.level < r.initialLevel) Note("ความแรง L${r.initialLevel}→L${r.level}")
            }
        HeadMap(
            r.dose.filterValues { it > 0 }.keys.toList(),
            Modifier.align(Alignment.CenterHorizontally).width(190.dp),
            values = r.dose.mapValues { if (peak > 0) it.value / peak else 0.0 },
        )
        if (used.isNotEmpty())
            Note(
                "${used.size} จุด · สูงสุด ${"%.2f".format(peak)} J/cm²",
                Modifier.align(Alignment.CenterHorizontally),
            )
        if (r.items.size > 1)
            r.items.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Label(item.name, size = 13)
                    Label(timeText(item.seconds.toDouble()), size = 12, mono = true)
                }
            }
        Action("Back to Home") { a.navigate(Screen.HOME) }
    }
}

@Composable
fun StatTiles(items: List<Pair<String, String>>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (v, l) ->
            Column(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(C.surface)
                    .border(1.dp, C.line, RoundedCornerShape(16.dp))
                    .padding(vertical = 11.dp, horizontal = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Label(v, size = 20, display = true)
                Label(l, size = 11, color = C.muted, align = TextAlign.Center)
            }
        }
    }
}

@Composable
fun HeadsetCheck(a: AppState) {
    val model = remember { HeadsetCheckModel(a.wasHeadsetCheckedToday()) }
    var frame by remember { mutableIntStateOf(0) }
    val values = remember(frame) { model.values.toMap() }
    val ok = model.ready
    LaunchedEffect(model) {
        var last = SystemClock.elapsedRealtime()
        while (true) {
            delay(100)
            val now = SystemClock.elapsedRealtime()
            model.advance((now - last) / 1000.0)
            last = now
            frame++
            if (model.automatic) {
                a.checkComplete(model.values)
                break
            }
        }
    }
    Column(
        Modifier.fillMaxSize()
            .background(C.bg)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Note("EEG Closed-loop")
            RoundAction("✕", "ปิด") { a.checkOpen = false }
        }
        Title("Headset check", size = 20)
        Note(
            "F3 · F4 · Fz · Cz · Pz + Ref + GND" +
                if (model.previouslyChecked) " · เช็คแล้ววันนี้" else ""
        )
        HeadsetDiagram(values, Modifier.width(250.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            model.channels.forEach { n -> CheckTile(n, values.getValue(n), Modifier.weight(1f)) }
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Note("50 Hz")
            Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(C.surface2)) {
                Box(
                    Modifier.fillMaxHeight()
                        .fillMaxWidth((model.hum / 4).toFloat().coerceIn(.04f, 1f))
                        .background(if (model.hum < 1.5) C.ok else Color(0xFF9A6200))
                )
            }
            Label("${"%.1f".format(model.hum)} µV", size = 12, color = C.muted, mono = true)
        }
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(C.surface2)
                .padding(horizontal = 12.dp, vertical = 9.dp)
                .heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Label(if (ok) "✓" else "!", color = if (ok) C.ok else Color(0xFF9A6200))
            val channel = model.fixChannel
            val instruction =
                when (channel) {
                    "Ref" -> "หนีบคลิปติ่งหูให้แน่น"
                    "GND" -> "กดขั้วหน้าผากให้แนบ"
                    else -> "เลื่อนผมออก กดขั้วให้แนบหนังศีรษะ"
                }
            Label(
                when {
                    ok -> "พร้อมแล้ว · ทุกขั้วต่ำกว่า 10 kΩ"
                    channel != null ->
                        "$channel ${"%.1f".format(values.getValue(channel))} kΩ · $instruction"
                    model.needsHumFix ->
                        "ไฟฟ้ารบกวน 50 Hz สูง · ถอดสายชาร์จ ห่างปลั๊กไฟ กดคลิปติ่งหู"
                    else -> "กำลังวัดค่าความต้านทาน รอให้ต่ำกว่า 10 kΩ"
                },
                Modifier.weight(1f),
                size = 12.5,
            )
            if (!ok && (channel != null || model.needsHumFix))
                Pill(if (channel != null) "ปรับแล้ว" else "แก้แล้ว") {
                    model.fix()
                    frame++
                }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Label("● <10 kΩ", size = 10, color = C.ok)
            Label("● 10–20", size = 10, color = Color(0xFF9A6200))
            Label("● >20", size = 10, color = C.accent)
        }
        Action(if (ok) "สัญญาณดี · เปิด Closed-loop" else "กำลังเช็ค…", enabled = ok) {
            a.checkComplete(values)
        }
        Note(
            "Closed-loop ใช้ EEG ระหว่างพักเพื่อปรับแสง ต้องให้ทุกขั้วต่ำกว่า 10 kΩ ก่อน",
            Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CheckTile(n: String, v: Double, modifier: Modifier) {
    val color = if (v < 10) C.ok else if (v < 20) Color(0xFF9A6200) else C.accent
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(C.surface2)
            .border(1.5.dp, color, RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Label(n, size = 14, color = color, display = true)
        Label("${"%.1f".format(v)}", size = 10.5, color = C.muted, mono = true)
    }
}

@Composable
private fun HeadsetDiagram(values: Map<String, Double>, modifier: Modifier) {
    val p = C
    val positions =
        linkedMapOf(
            "F3" to (68f to 80f),
            "F4" to (152f to 80f),
            "Fz" to (110f to 66f),
            "Cz" to (110f to 114f),
            "Pz" to (110f to 158f),
            "GND" to (110f to 38f),
            "Ref" to (20f to 110f),
        )
    BoxWithConstraints(modifier.aspectRatio(220f / 196f)) {
        val w = maxWidth
        val pointScale = w / 220
        Canvas(Modifier.fillMaxSize()) {
            val factor = size.width / 220f
            scale(factor, factor, pivot = Offset.Zero) {
                drawOval(p.surface2, Offset(11f, 93f), androidx.compose.ui.geometry.Size(18f, 34f))
                drawOval(
                    p.line,
                    Offset(11f, 93f),
                    androidx.compose.ui.geometry.Size(18f, 34f),
                    style = Stroke(1f),
                )
                drawOval(p.surface2, Offset(191f, 93f), androidx.compose.ui.geometry.Size(18f, 34f))
                drawOval(
                    p.line,
                    Offset(191f, 93f),
                    androidx.compose.ui.geometry.Size(18f, 34f),
                    style = Stroke(1f),
                )
                drawCircle(p.surface2, 84f, Offset(110f, 106f))
                drawCircle(p.line, 84f, Offset(110f, 106f), style = Stroke(1.5f))
                drawPath(
                    Path().apply {
                        moveTo(100f, 24f)
                        lineTo(110f, 8f)
                        lineTo(120f, 24f)
                        close()
                    },
                    p.line,
                    style = Stroke(1f),
                )
                positions.forEach { (n, xy) ->
                    val v = values.getValue(n)
                    val color =
                        if (v < 10) p.ok else if (v < 20) Color(0xFF9A6200) else Color(0xFFB13E2C)
                    val center = Offset(xy.first, xy.second)
                    val small = n == "GND" || n == "Ref"
                    if (v >= 10)
                        drawCircle(color.copy(alpha = .18f), if (small) 16f else 24f, center)
                    drawCircle(
                        color.copy(alpha = .22f).compositeOver(p.surface),
                        if (small) 10f else 16f,
                        center,
                    )
                    drawCircle(color, if (small) 10f else 16f, center, style = Stroke(2f))
                }
            }
        }
        positions.forEach { (n, xy) ->
            val small = n == "GND" || n == "Ref"
            Box(
                Modifier.offset(pointScale * (xy.first - 18f), pointScale * (xy.second - 12f))
                    .size(pointScale * 36f, pointScale * 24f),
                contentAlignment = Alignment.Center,
            ) {
                Label(
                    if (n == "Ref") "A1" else n,
                    size = (if (small) 8 else 10) * (w.value / 220),
                    mono = true,
                    weight = FontWeight.SemiBold,
                )
            }
        }
        Label(
            "Ref: A1 (ear) · GND: AFz",
            Modifier.align(Alignment.BottomCenter),
            size = 8 * (w.value / 220),
            color = p.muted,
            mono = true,
        )
    }
}
