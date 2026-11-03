@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.brainstem.stimulate

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlin.math.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val BodyFont =
    FontFamily(
        Font(R.font.plex_regular),
        Font(R.font.plex_medium, FontWeight.Medium),
        Font(R.font.plex_semibold, FontWeight.SemiBold),
    )
val DisplayFont = FontFamily(Font(R.font.mitr_medium, FontWeight.Medium))
val MonoFont = FontFamily(Font(R.font.plex_mono))

data class Palette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val ok: Color,
)

val Light =
    Palette(
        Color(0xFFE9EFEC),
        Color.White,
        Color(0xFFEEF3F1),
        Color(0xFF13302A),
        Color(0xFF5F756F),
        Color(0xFFD8E2DE),
        Color(0xFFD9533F),
        Color(0xFF2C7A4C),
    )
val Dark =
    Palette(
        Color(0xFF0A1110),
        Color(0xFF16221F),
        Color(0xFF1C2B27),
        Color(0xFFE1ECE8),
        Color(0xFF93A9A2),
        Color(0xFF2A3B37),
        Color(0xFFF07A68),
        Color(0xFF62C189),
    )
val LocalPalette = staticCompositionLocalOf { Light }
val C: Palette
    @Composable get() = LocalPalette.current

@Composable
fun BrainTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) Dark else Light
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme =
                if (dark)
                    darkColorScheme(
                        primary = p.accent,
                        background = p.bg,
                        surface = p.surface,
                        onSurface = p.ink,
                    )
                else
                    lightColorScheme(
                        primary = p.accent,
                        background = p.bg,
                        surface = p.surface,
                        onSurface = p.ink,
                    ),
            typography =
                Typography(
                    bodyLarge = TextStyle(fontFamily = BodyFont, fontSize = 14.sp, color = p.ink),
                    bodyMedium = TextStyle(fontFamily = BodyFont, fontSize = 13.sp, color = p.ink),
                ),
        ) {
            ProvideTextStyle(
                LocalTextStyle.current.copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle =
                        androidx.compose.ui.text.style.LineHeightStyle(
                            androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                            androidx.compose.ui.text.style.LineHeightStyle.Trim.None,
                        ),
                    letterSpacing = 0.sp,
                )
            ) {
                content()
            }
        }
    }
}

@Composable
fun Label(
    s: String,
    modifier: Modifier = Modifier,
    size: Number = 14,
    color: Color = C.ink,
    weight: FontWeight = FontWeight.Normal,
    display: Boolean = false,
    mono: Boolean = false,
    align: TextAlign? = null,
) {
    Text(
        s,
        modifier,
        color = color,
        fontSize = size.toFloat().sp,
        fontWeight = weight,
        fontFamily = if (display) DisplayFont else if (mono) MonoFont else BodyFont,
        lineHeight = (size.toFloat() * 1.55f).sp,
        textAlign = align,
    )
}

@Composable
fun Note(s: String, modifier: Modifier = Modifier) =
    Label(s, modifier, size = 12.5, color = C.muted)

@Composable
fun Title(s: String, modifier: Modifier = Modifier, size: Number = 17) =
    Label(s, modifier, size = size, weight = FontWeight.Medium, display = true)

@Composable
fun CardBox(
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    padding: Int = 16,
    radius: Int = 22,
    gap: Int = 10,
    bottomPadding: Int = padding,
    topPadding: Int = padding,
    insetBorder: Boolean = false,
    headGradient: Boolean = false,
    outlined: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius.dp))
            .then(
                if (accent)
                    Modifier.background(
                        Brush.linearGradient(
                            0f to C.accent.copy(alpha = .11f).compositeOver(C.surface),
                            .65f to C.surface,
                        )
                    )
                else Modifier.background(C.surface)
            )
            .then(
                if (headGradient) Modifier.referenceHeadGradient(C.surface, C.accent) else Modifier
            )
            .border(
                1.dp,
                if (!outlined) Color.Transparent
                else if (accent) C.accent.copy(alpha = .28f).compositeOver(C.line) else C.line,
                RoundedCornerShape(radius.dp),
            )
            .padding(
                start = (padding + if (insetBorder || !outlined) 0 else 1).dp,
                end = (padding + if (insetBorder || !outlined) 0 else 1).dp,
                top = (topPadding + if (insetBorder || !outlined) 0 else 1).dp,
                bottom = (bottomPadding + if (insetBorder || !outlined) 0 else 1).dp,
            ),
        verticalArrangement = Arrangement.spacedBy(gap.dp),
        content = content,
    )
}

fun Modifier.referenceHeadGradient(surface: Color, accent: Color) = drawWithCache {
    val rx = size.width * 1.2f
    val sy = (size.height * .9f / rx).coerceAtLeast(.01f)
    val brush =
        Brush.radialGradient(
            0f to accent.copy(alpha = .1f).compositeOver(surface),
            .7f to surface,
            center = Offset(size.width / 2, 0f),
            radius = rx,
        )
    onDrawBehind {
        scale(1f, sy, pivot = Offset.Zero) {
            drawRect(brush, size = Size(size.width, size.height / sy))
        }
    }
}

@Composable
fun RoundAction(
    text: String,
    label: String = text,
    enabled: Boolean = true,
    size: Int = 36,
    color: Color = C.ink,
    filled: Boolean = false,
    background: Color = C.surface,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(size.dp)
            .clip(CircleShape)
            .background(if (filled) C.accent else background)
            .border(1.dp, if (filled) C.accent else C.line, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label }
            .then(if (enabled) Modifier else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 20.sp,
            lineHeight = 20.sp,
            fontFamily = FontFamily.SansSerif,
            color = if (!enabled) C.muted.copy(alpha = .35f) else if (filled) Color.White else color,
        )
    }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) selectionColor() else C.surface)
            .border(1.dp, if (selected) selectionColor() else C.line, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Label(text, size = 12, color = if (selected) selectionInk() else C.muted)
    }
}

@Composable
fun ChipRow(items: List<String>, selected: String, onClick: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { Pill(it, selected = it == selected) { onClick(it) } }
    }
}

@Composable
fun FrequencyChip(text: String, selected: Boolean, band: Color, onClick: () -> Unit) {
    Box(
        Modifier.clip(CircleShape)
            .background(if (selected) band else C.surface)
            .border(1.dp, if (selected) band else C.line, CircleShape)
            .drawBehind { drawRect(band, size = Size(3.dp.toPx(), size.height)) }
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Label(text, size = 12, color = if (selected) Color.White else C.muted)
    }
}

@Composable
fun Segments(
    items: List<String>,
    selected: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    surfaceSelection: Boolean = false,
    onClick: (String) -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(C.surface2)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        items.forEach { v ->
            Box(
                Modifier.weight(1f)
                    .then(
                        if (surfaceSelection && v == selected)
                            Modifier.shadow(2.dp, RoundedCornerShape(10.dp))
                        else Modifier
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (v == selected) {
                            if (surfaceSelection) C.surface else selectionColor()
                        } else Color.Transparent
                    )
                    .clickable { onClick(v) }
                    .padding(vertical = if (compact) 6.dp else 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Label(
                    v,
                    size = if (compact) 12.5 else 13.5,
                    weight = if (v == selected) FontWeight.Medium else FontWeight.Normal,
                    color =
                        if (surfaceSelection) {
                            if (v == selected) C.ink else C.muted
                        } else if (v == selected) selectionInk() else C.ink,
                )
            }
        }
    }
}

@Composable fun selectionColor() = if (C.bg == Dark.bg) Color(0xFFF5A394) else Color(0xFFC8452F)

@Composable fun selectionInk() = if (C.bg == Dark.bg) Color(0xFFC8452F) else Color.White

@Composable
fun EvidenceBadge(kind: String, text: String) {
    val color =
        if (kind == "pilot") C.ok else if (C.bg == Dark.bg) Color(0xFFE3A63D) else Color(0xFF9A6200)
    Label(
        text,
        Modifier.clip(CircleShape)
            .background(color.copy(alpha = .15f))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        size = 11,
        color = color,
    )
}

fun Modifier.dashedBorder(color: Color, radius: Dp = 16.dp) = drawBehind {
    drawRoundRect(
        color,
        cornerRadius = CornerRadius(radius.toPx()),
        style =
            Stroke(
                1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
            ),
    )
}

@Composable
fun Toggle(label: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = C
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(p.surface)
            .border(1.dp, p.line, RoundedCornerShape(50))
            .toggleable(value = on, role = Role.Switch, onValueChange = { onClick() })
            .padding(start = 5.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.width(26.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(50))
                .background(if (on) p.accent else p.surface2)
        ) {
            Box(
                Modifier.align(if (on) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(2.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(.5.dp, p.line, CircleShape)
            )
        }
        Label(label, size = 12, color = p.muted)
    }
}

@Composable
fun Stepper(
    value: Double,
    unit: String,
    min: Double,
    max: Double,
    step: Double = 1.0,
    narrow: Boolean = false,
    large: Boolean = false,
    small: Boolean = false,
    onChange: (Double) -> Unit,
) {
    val compact = unit.isBlank()
    Row(
        Modifier.then(if (small) Modifier.fillMaxWidth() else Modifier)
            .clip(RoundedCornerShape(50))
            .border(1.dp, C.line, RoundedCornerShape(50))
            .padding(if (compact) 3.dp else if (narrow) 4.dp else 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement =
            if (small) Arrangement.SpaceBetween
            else Arrangement.spacedBy(if (compact || narrow) 2.dp else 10.dp),
    ) {
        SmallStep(
            "−",
            "ลด $unit",
            value > min,
            if (compact) 24 else if (large) 38 else if (narrow || small) 30 else 32,
        ) {
            onChange((value - step).coerceIn(min, max))
        }
        Label(
            num(value),
            Modifier.widthIn(
                min =
                    if (large) 44.dp
                    else if (small) 28.dp else if (compact || narrow) 26.dp else 30.dp
            ),
            size =
                if (compact) 12.5 else if (large) 22 else if (small) 17 else if (narrow) 19 else 18,
            weight = FontWeight.Medium,
            mono = compact,
            display = !compact,
            align = TextAlign.Center,
            color = if (compact) C.accent else C.ink,
        )
        if (!compact) Label(unit, size = if (narrow) 11 else 12.5, color = C.muted)
        SmallStep(
            "+",
            "เพิ่ม $unit",
            value < max,
            if (compact) 24 else if (large) 38 else if (narrow || small) 30 else 32,
        ) {
            onChange((value + step).coerceIn(min, max))
        }
    }
}

@Composable
private fun SmallStep(s: String, label: String, enabled: Boolean, size: Int, onClick: () -> Unit) {
    val latestClick by rememberUpdatedState(onClick)
    Box(
        Modifier.size(size.dp)
            .clip(CircleShape)
            .background(C.surface2)
            .pointerInput(enabled) {
                if (enabled)
                    coroutineScope {
                        detectTapGestures(
                            onPress = {
                                var repeating = false
                                val repeatJob = launch {
                                    delay(450)
                                    repeating = true
                                    while (true) {
                                        latestClick()
                                        delay(90)
                                    }
                                }
                                val released = tryAwaitRelease()
                                repeatJob.cancel()
                                if (released && !repeating) latestClick()
                            }
                        )
                    }
            }
            .semantics {
                contentDescription = label
                role = Role.Button
                if (!enabled) disabled()
                onClick {
                    if (enabled) latestClick()
                    enabled
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Label(
            s,
            size = if (size == 24) 14 else 17,
            color = if (enabled) C.ink else C.muted.copy(alpha = .35f),
        )
    }
}

@Composable
fun Action(
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(if (primary) 16.dp else 18.dp))
            .background(if (primary) C.accent.copy(alpha = if (enabled) 1f else .4f) else C.surface)
            .border(
                if (primary) 0.dp else 1.dp,
                if (primary) Color.Transparent else C.line,
                RoundedCornerShape(18.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        val color = if (primary) Color.White else C.ink
        val subtitleAt = text.indexOf("  ")
        if (text.startsWith("Start Session") && subtitleAt >= 0) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Label(
                    "Start Session",
                    size = 16,
                    color = color,
                    display = true,
                    weight = FontWeight.Medium,
                )
                Label(
                    text.substring(subtitleAt).trim(),
                    size = 12.5,
                    color = color.copy(alpha = .8f),
                )
            }
        } else {
            Text(
                buildAnnotatedString {
                    append(text)
                    if (primary && subtitleAt >= 0)
                        addStyle(
                            SpanStyle(
                                fontFamily = BodyFont,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = color.copy(alpha = .8f),
                            ),
                            subtitleAt,
                            text.length,
                        )
                },
                color = color,
                fontFamily = if (!primary && text == "Save") BodyFont else DisplayFont,
                fontSize = if (!primary && text == "Save") 14.sp else 16.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.8.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun OptionRow(label: String, content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Note(label)
        content()
    }
}

@Composable
fun Timeline(
    steps: List<Step>,
    modifier: Modifier = Modifier,
    selected: Int = -1,
    runStyle: Boolean = false,
    big: Boolean = false,
) {
    val ink = C.ink
    Row(
        modifier.fillMaxWidth().height(if (runStyle) 7.dp else if (big) 10.dp else 8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { i, s ->
            Box(
                Modifier.weight(s.dur.toFloat())
                    .requiredHeight(
                        if (runStyle) 7.dp
                        else if (i == selected) {
                            if (big) 16.dp else 12.8.dp
                        } else if (big) 10.dp else 8.dp
                    )
                    .then(
                        if (runStyle && i == selected)
                            Modifier.drawBehind {
                                drawRoundRect(
                                    ink,
                                    topLeft = Offset(-2.dp.toPx(), -2.dp.toPx()),
                                    size =
                                        Size(size.width + 4.dp.toPx(), size.height + 4.dp.toPx()),
                                    cornerRadius = CornerRadius(4.dp.toPx()),
                                    style = Stroke(2.dp.toPx()),
                                )
                            }
                        else Modifier
                    )
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (s.phase.isNotEmpty()) SolidColor(C.muted.copy(alpha = .3f))
                        else
                            stepBrush(s, if (i == selected) 1f else if (selected < 0) .9f else .45f)
                    )
            )
        }
    }
}

fun stepBrush(step: Step, alpha: Float = 1f): Brush =
    if (step.mode == "sweep")
        Brush.horizontalGradient(
            listOf(
                Color(bandColor(step.f1)).copy(alpha = alpha),
                Color(bandColor(step.f2)).copy(alpha = alpha),
            )
        )
    else SolidColor(Color(bandColor(step.f)).copy(alpha = alpha))

@Composable
fun Wordmark(size: Int = 20, activeModule: Boolean = true) {
    val p = C
    Text(
        buildAnnotatedString {
            append("Brain")
            listOf(
                    "S" to if (p.bg == Dark.bg) Dark.accent else Light.accent,
                    "T" to
                        if (activeModule) p.muted.copy(alpha = .45f)
                        else Color(if (p.bg == Dark.bg) 0xFFA08BEA else 0xFF6E4FC0),
                    "E" to
                        if (activeModule) p.muted.copy(alpha = .45f)
                        else Color(if (p.bg == Dark.bg) 0xFF6FA0E8 else 0xFF2F64AD),
                    "M" to
                        if (activeModule) p.muted.copy(alpha = .45f)
                        else Color(if (p.bg == Dark.bg) 0xFF4DBFA6 else 0xFF1F8277),
                )
                .forEach { (s, c) ->
                    withStyle(SpanStyle(color = c, fontWeight = FontWeight.SemiBold)) { append(s) }
                }
        },
        fontFamily = DisplayFont,
        fontSize = size.sp,
        color = p.ink,
    )
}

val ICONS =
    mapOf(
        "leaf" to "M5 19C5 11 11 6 19 5C19 13 14 19 6 19M5 19C8 15 11 13 14 12",
        "moon" to "M20 14.5A8 8 0 1 1 9.5 4A6.5 6.5 0 0 0 20 14.5Z",
        "target" to
            "M20 12A8 8 0 1 1 4 12A8 8 0 1 1 20 12M16 12A4 4 0 1 1 8 12A4 4 0 1 1 16 12M13 12A1 1 0 1 1 11 12A1 1 0 1 1 13 12",
        "balance" to "M12 4V20M7 20H17M5 8H19M5 8L2.5 14H7.5ZM19 8L16.5 14H21.5Z",
        "wave" to "M2 12C4 8 5.5 8 7 12S10 16 12 12S15 8 17 12S20 16 22 12",
        "pulse" to "M3 12H7L9 7L13 17L15 12H21",
        "bolt" to "M13 2L4 14H11L10 22L19 10H12Z",
        "brain" to
            "M9 4A3 3 0 0 0 6 7A3 3 0 0 0 4 12A3 3 0 0 0 6 17A3 3 0 0 0 12 18V5A2 2 0 0 0 9 4ZM15 4A3 3 0 0 1 18 7A3 3 0 0 1 20 12A3 3 0 0 1 18 17A3 3 0 0 1 12 18",
        "sprout" to
            "M12 20V12M12 12C12 8 9 6 5 6C5 10 8 12 12 12ZM12 10C12 7 14.5 5 18 5C18 8.5 15.5 10 12 10Z",
        "spark" to
            "M12 3V7M12 17V21M3 12H7M17 12H21M6 6L8.5 8.5M15.5 15.5L18 18M6 18L8.5 15.5M15.5 8.5L18 6",
        "sun" to
            "M16 12A4 4 0 1 1 8 12A4 4 0 1 1 16 12M12 2V4M12 20V22M2 12H4M20 12H22M4.9 4.9L6.3 6.3M17.7 17.7L19.1 19.1M4.9 19.1L6.3 17.7M17.7 6.3L19.1 4.9",
        "settings" to
            "M4 6H13M17 6H20M4 12H7M11 12H20M4 18H15M19 18H20M17 6A2 2 0 1 1 13 6A2 2 0 1 1 17 6M11 12A2 2 0 1 1 7 12A2 2 0 1 1 11 12M19 18A2 2 0 1 1 15 18A2 2 0 1 1 19 18",
        "star" to
            "M12 3.5L14.6 8.8L20.5 9.7L16.2 13.8L17.2 19.6L12 16.9L6.8 19.6L7.8 13.8L3.5 9.7L9.4 8.8Z",
        "back" to "M15 5L8 12L15 19",
        "eye" to "M2 12C6 3 18 3 22 12C18 21 6 21 2 12ZM15 12A3 3 0 1 1 9 12A3 3 0 1 1 15 12",
        "eyeoff" to
            "M3 3L21 21M2 12C4 8 7 6 10 6M14 6C18 7 20 9 22 12C20 16 17 18 14 18M10 18C6 17 4 15 2 12",
    )

@Composable
fun LineIcon(name: String, modifier: Modifier = Modifier, color: Color = C.accent) {
    REFERENCE_ICONS[name]?.let { resource ->
        Icon(
            painterResource(resource),
            contentDescription = null,
            modifier = modifier.size(22.dp),
            tint = color,
        )
        return
    }
    val path =
        remember(name) {
            PathParser().parsePathString(ICONS[name] ?: ICONS.getValue("sun")).toPath()
        }
    Canvas(modifier.size(22.dp)) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            drawPath(
                path,
                color,
                style = Stroke(1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

@Composable
fun HeadGlow(points: List<String>, values: Map<String, Double>, modifier: Modifier = Modifier) {
    val p = C
    val dark = p.bg == Dark.bg
    Canvas(modifier.width(270.dp).aspectRatio(1f)) {
        val sx = size.width / 220f
        // The source SVG is centered inside a square run panel.
        translate(top = (size.height - size.width / 1.1f) / 2) {
            scale(sx, sx, pivot = Offset.Zero) {
                val outline = if (dark) Color.White.copy(alpha = .16f) else p.line
                val fill = if (dark) Color.White.copy(alpha = .07f) else p.surface2
                drawPath(
                    Path().apply {
                        moveTo(98f, 16f)
                        quadraticTo(110f, 2f, 122f, 16f)
                        moveTo(24f, 92f)
                        quadraticTo(14f, 106f, 24f, 120f)
                        moveTo(196f, 92f)
                        quadraticTo(206f, 106f, 196f, 120f)
                    },
                    outline,
                    style = Stroke(2f, cap = StrokeCap.Round),
                )
                val rect = Rect(24f, 14f, 196f, 198f)
                drawOval(fill, rect.topLeft, rect.size)
                drawOval(outline, rect.topLeft, rect.size, style = Stroke(1.5f))
                clipPath(Path().apply { addOval(rect) }) {
                    POINTS.forEach { n ->
                        val xy = POINT_XY[POINTS.indexOf(n)]
                        val value = (values[n] ?: 0.0).toFloat()
                        if (value > 0)
                            drawCircle(
                                Brush.radialGradient(
                                    0f to Color(0xFFFF9A8A),
                                    .4f to Color(0xFFFF6B5B).copy(alpha = .55f),
                                    1f to Color(0xFFFF6B5B).copy(alpha = 0f),
                                    center = Offset(xy.first, xy.second),
                                    radius = 34f,
                                ),
                                34f,
                                Offset(xy.first, xy.second),
                                alpha = min(1f, value * if (dark) 1.7f else 1.25f),
                                blendMode = if (dark) BlendMode.Screen else BlendMode.SrcOver,
                            )
                    }
                }
                POINTS.forEach { n ->
                    val xy = POINT_XY[POINTS.indexOf(n)]
                    val active = n in points
                    drawCircle(
                        if (active) {
                            if (dark) Color.White else Color(0xFFB13E2C)
                        } else if (dark) Color.White.copy(alpha = .28f) else p.muted,
                        if (active) 3f else 1.8f,
                        Offset(xy.first, xy.second),
                        alpha = if (active) .95f else .6f,
                    )
                }
            }
        }
    }
}

@Composable
fun HeadMap(
    points: List<String>,
    modifier: Modifier = Modifier,
    glow: Boolean = false,
    values: Map<String, Double> = emptyMap(),
    onToggle: ((String) -> Unit)? = null,
) {
    val p = C
    BoxWithConstraints(
        modifier.width(240.dp).aspectRatio(1.1f).semantics {
            contentDescription = "แผนผังจุดกระตุ้น 19 จุด"
        }
    ) {
        val w = maxWidth
        val scale = w / 220
        Canvas(Modifier.fillMaxSize()) {
            val sx = size.width / 220f
            scale(sx, sx, pivot = Offset.Zero) {
                val outline = p.line
                drawOval(p.surface2, Offset(22f, 18f), Size(176f, 176f))
                drawOval(outline, Offset(22f, 18f), Size(176f, 176f), style = Stroke(1.4f))
                drawOval(p.surface2, Offset(10f, 91f), Size(16f, 30f))
                drawOval(outline, Offset(10f, 91f), Size(16f, 30f), style = Stroke(1f))
                drawOval(p.surface2, Offset(194f, 91f), Size(16f, 30f))
                drawOval(outline, Offset(194f, 91f), Size(16f, 30f), style = Stroke(1f))
                drawPath(
                    Path().apply {
                        moveTo(100f, 22f)
                        lineTo(110f, 6f)
                        lineTo(120f, 22f)
                        close()
                    },
                    outline,
                    style = Stroke(1f),
                )
                if (!glow) {
                    drawLine(
                        outline,
                        Offset(22f, 106f),
                        Offset(198f, 106f),
                        1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 4f)),
                    )
                    drawLine(
                        outline,
                        Offset(110f, 18f),
                        Offset(110f, 194f),
                        1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 4f)),
                    )
                }
                POINTS.forEachIndexed { i, n ->
                    val (x, y) = POINT_XY[i]
                    val on = n in points
                    if (on) {
                        val alpha = values[n]?.toFloat()?.coerceIn(0f, 1f) ?: 1f
                        drawCircle(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFFFF6B5B).copy(alpha = .7f * (.35f + .65f * alpha)),
                                    Color.Transparent,
                                ),
                                Offset(x, y),
                                if (glow) 34f else 13f + alpha * 11,
                            ),
                            if (glow) 34f else 13f + alpha * 11,
                            Offset(x, y),
                        )
                    }
                    if (glow)
                        drawCircle(
                            if (on) p.accent else p.muted.copy(alpha = .4f),
                            if (on) 2.6f else 1.6f,
                            Offset(x, y),
                        )
                    else {
                        val value = values[n]?.toFloat()?.coerceIn(0f, 1f) ?: 1f
                        drawCircle(
                            if (on) Color(0xFFE8463C).copy(alpha = .45f + .55f * value)
                            else p.surface,
                            11f,
                            Offset(x, y),
                        )
                        drawCircle(
                            if (on && onToggle != null) {
                                if (p.bg == Dark.bg) Color(0xFFF5A394) else Color(0xFFC8452F)
                            } else if (on) Color(0xFFB13E2C) else outline,
                            11f,
                            Offset(x, y),
                            style = Stroke(if (on && onToggle != null) 2.5f else 1.2f),
                        )
                    }
                }
            }
        }
        if (!glow)
            POINTS.forEachIndexed { i, n ->
                val (x, y) = POINT_XY[i]
                Box(
                    Modifier.offset(scale * (x - 12f), scale * (y - 12f))
                        .size(scale * 24f)
                        .clip(CircleShape)
                        .then(
                            if (onToggle != null)
                                Modifier.clickable(role = Role.Checkbox) { onToggle(n) }
                                    .semantics {
                                        contentDescription = n
                                        selected = n in points
                                    }
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        n,
                        fontSize = (7.5f * (w.value / 220)).sp,
                        color = if (n in points) Color.White else p.muted,
                        fontFamily = MonoFont,
                        fontWeight = if (n in points) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
    }
}

@Composable
fun Expandable(
    title: String,
    colors: List<Color> = emptyList(),
    compactDisclosure: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(
                    vertical = if (compactDisclosure) 0.dp else 8.dp,
                    horizontal = if (compactDisclosure) 0.dp else 2.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (compactDisclosure)
                Label(if (expanded) "▾ $title" else "▸ $title", size = 12.5, color = C.accent)
            else Label(title, size = 13, color = C.muted)
            if (colors.isNotEmpty())
                Row(Modifier.weight(1f).height(6.dp).clip(CircleShape)) {
                    colors.forEach { Box(Modifier.weight(1f).fillMaxHeight().background(it)) }
                }
            else Spacer(Modifier.weight(1f))
            if (!compactDisclosure) {
                val color = C.muted
                Canvas(Modifier.size(16.dp)) {
                    val path =
                        Path().apply {
                            moveTo(size.width * .25f, size.height * if (expanded) .625f else .375f)
                            lineTo(size.width * .5f, size.height * if (expanded) .375f else .625f)
                            lineTo(size.width * .75f, size.height * if (expanded) .625f else .375f)
                        }
                    drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                }
            }
        }
        if (expanded) content()
    }
}
