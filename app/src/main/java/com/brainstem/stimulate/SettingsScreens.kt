@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.brainstem.stimulate

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(a: AppState) {
    ScreenColumn("settings") {
        SettingsSection("Mode")
        CardBox(radius = 16, gap = 8, padding = 16) {
            Segments(listOf("Easy", "Pro"), if (a.pro) "Pro" else "Easy") {
                a.pro = it == "Pro"
                a.save()
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(
                        "bulb" to "Level 1–10",
                        "clock" to "Duty cycle",
                        "edit" to "สร้าง / แก้โปรแกรม",
                        "brain" to "Helmet spec",
                    )
                    .forEach { (icon, label) ->
                        Row(
                            Modifier.clip(RoundedCornerShape(10.dp))
                                .background(C.surface2)
                                .then(
                                    if (a.pro)
                                        Modifier.border(1.dp, C.accent, RoundedCornerShape(10.dp))
                                    else Modifier
                                )
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            LineIcon(icon, Modifier.size(16.dp), if (a.pro) C.accent else C.muted)
                            Label(label, size = 12, color = if (a.pro) C.ink else C.muted)
                        }
                    }
            }
            Note(
                if (a.pro) "เปิดอยู่ · เห็นใน Quick Start และหน้าโปรแกรม"
                else "Easy ล็อก Level 1 · duty ค่าเริ่มต้น"
            )
        }
        SettingsSection("Simulation")
        CardBox(radius = 16, gap = 8, padding = 16) {
            Segments(
                listOf("Demo ×20", "Real time"),
                if (a.speed == 20) "Demo ×20" else "Real time",
            ) {
                a.speed = if (it == "Demo ×20") 20 else 1
                a.save()
            }
            Note("เครื่องต้นแบบ · เลือกเวลาจริงเมื่อต่อหมวกจริง")
        }
        if (a.pro) {
            SettingsSection("Helmet")
            CardBox(radius = 16, gap = 8, padding = 16) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        NumberSetting("Wavelength (nm)", a.spec.wavelength, 600..1100) {
                            a.spec = a.spec.copy(wavelength = it)
                            a.save()
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        NumberSetting("Irradiance at 100% (mW/cm²)", a.spec.irradiance, 1..200) {
                            a.spec = a.spec.copy(irradiance = it)
                            a.save()
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        NumberSetting("Default duty (%)", a.spec.duty, 10..90, 10) {
                            a.spec = a.spec.copy(duty = it)
                            a.save()
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        NumberSetting("Daily max (min)", a.spec.dailyMax, 5..120) {
                            a.spec = a.spec.copy(dailyMax = it)
                            a.save()
                        }
                    }
                }
                Note("ใช้คำนวณปริมาณแสง (J/cm²) และส่งให้หมวก")
            }
            CardBox(radius = 16, gap = 8, padding = 16) {
                Expandable("Developer · ESP32 commands & references", compactDisclosure = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Label(
                            "// แอป → หมวก (BLE) ส่งครั้งเดียวต่อขั้น หมวกจับเวลาเอง\n{\"cmd\":\"step\",\"mask\":\"0x7FFFF\",\"mode\":\"sweep\",\"f1\":24,\"f2\":35,\"df\":1,\"dt\":55,\"duty\":${a.spec.duty},\"pat\":\"sync\",\"dur\":660}\n{\"cmd\":\"pause\"} {\"cmd\":\"resume\"} {\"cmd\":\"stop\"}",
                            size = 10,
                            mono = true,
                        )
                        Note(
                            "mask = บิต 0–18 เรียงตาม ${POINTS.joinToString(" ")} · BLE หลุดเกิน 3 วินาที หมวกดับแสงเอง"
                        )
                        Note(
                            "Native build: simulation only. No BLE service UUID or characteristic was provided."
                        )
                        listOf(
                                "iMediSync. PBM Protocol Guide (iSyncWave)",
                                "Gracefire Guide to Neuromodulation: Vielight NeuroPro 2",
                                "Hamblin MR (2016). BBA Clinical 6:113–124.",
                                "Salehpour F, et al. (2018). Mol Neurobiol 55:6601–6636.",
                                "Zomorrodi R, et al. (2019). Sci Rep 9:6309.",
                            )
                            .forEach { Note(it) }
                    }
                }
            }
        }
        SettingsSection("Appearance")
        CardBox(radius = 16, gap = 8, padding = 16) {
            Segments(
                listOf("System", "Light", "Dark"),
                a.theme.replaceFirstChar { it.uppercase() },
            ) {
                a.theme = it.lowercase()
                a.save()
            }
        }
        SettingsSection("Account")
        CardBox(radius = 16, gap = 8, padding = 16) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Label(a.user?.name ?: "ผู้ใช้")
                    Note("${a.user?.age?:9} ปี")
                }
                Pill("เปลี่ยนผู้ใช้") { a.profileOpen = true }
            }
            Action("ประวัติการใช้งาน", primary = false) { a.navigate(Screen.HISTORY) }
            Action("ออกจากระบบ", primary = false) { a.logout() }
        }
        Note("BrainSTEM · Stimulate · native Android", Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun SettingsSection(text: String) {
    Text(
        text,
        Modifier.padding(top = 4.dp),
        fontFamily = BodyFont,
        fontSize = 11.5.sp,
        lineHeight = 17.825.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = .6.sp,
        color = C.muted,
    )
}

@Composable
private fun NumberSetting(
    label: String,
    value: Int,
    range: IntRange,
    step: Int = 1,
    onChange: (Int) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    var error by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Label(label, size = 12, color = C.muted)
        BasicTextField(
            text,
            { v ->
                text = v
                val n = v.toIntOrNull()
                error = n == null || n !in range || n % step != 0
                if (!error) onChange(n!!)
            },
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(C.surface2)
                .border(1.dp, if (error) C.accent else Color.Transparent, RoundedCornerShape(10.dp))
                .padding(horizontal = 11.dp, vertical = 9.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle =
                androidx.compose.ui.text.TextStyle(
                    fontFamily = BodyFont,
                    fontSize = 15.sp,
                    lineHeight = 23.25.sp,
                    color = C.ink,
                ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(C.accent),
        )
        if (error)
            Label(
                "${range.first}–${range.last}" + if (step > 1) " · ขั้นละ $step" else "",
                size = 11,
                color = C.accent,
            )
    }
}

@Composable
fun HistoryScreen(a: AppState) {
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var selected by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val ym = YearMonth.parse(month)
    val day = LocalDate.parse(selected)
    val today = LocalDate.now()
    fun date(r: SessionRecord) =
        Instant.ofEpochMilli(r.date).atZone(ZoneId.systemDefault()).toLocalDate()
    val thai = Locale.forLanguageTag("th-TH")
    ScreenColumn("history") {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundAction("‹", "เดือนก่อน") { month = ym.minusMonths(1).toString() }
            Title(
                "${ym.month.getDisplayName(java.time.format.TextStyle.FULL,thai)} ${ym.year+543}",
                size = 14.5,
            )
            RoundAction("›", "เดือนถัดไป") { month = ym.plusMonths(1).toString() }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("อา", "จ", "อ", "พ", "พฤ", "ศ", "ส").forEach {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Label(it, size = 11, color = C.muted)
                    }
                }
            }
            val offset = ym.atDay(1).dayOfWeek.value % 7
            val cells = (0 until offset).map { 0 } + (1..ym.lengthOfMonth()).toList()
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (week + List(7 - week.size) { 0 }).forEach { d ->
                        val dt = if (d > 0) ym.atDay(d) else null
                        val has = dt != null && a.history.any { date(it) == dt }
                        Column(
                            Modifier.weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (has) C.accent.copy(alpha = .08f) else Color.Transparent
                                )
                                .then(
                                    if (dt == day)
                                        Modifier.border(2.dp, C.accent, RoundedCornerShape(12.dp))
                                    else Modifier
                                )
                                .clickable(enabled = d > 0) { selected = dt.toString() },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement =
                                Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                        ) {
                            if (d > 0) {
                                Label(
                                    d.toString(),
                                    size = 14,
                                    color = if (dt == today) C.accent else C.ink,
                                    weight =
                                        if (dt == today) FontWeight.SemiBold else FontWeight.Normal,
                                )
                                if (has)
                                    Box(Modifier.size(6.dp).clip(CircleShape).background(C.accent))
                            }
                        }
                    }
                }
            }
        }
        val records = a.history.filter { date(it) == day }.asReversed()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Title(day.format(DateTimeFormatter.ofPattern("EEEE d MMM", thai)), size = 14)
            if (records.isNotEmpty())
                Note(
                    "${records.size} sessions · ${(records.sumOf{it.seconds}/60.0).roundToInt()} min"
                )
        }
        if (records.isEmpty())
            Note(
                "ไม่มีการใช้งานวันนี้",
                Modifier.align(Alignment.CenterHorizontally).padding(16.dp),
            )
        records.forEach { r ->
            CardBox(padding = 12, topPadding = 10, bottomPadding = 10, radius = 14) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Label(
                        Instant.ofEpochMilli(r.date)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("HH:mm")),
                        size = 13,
                        color = C.accent,
                        mono = true,
                    )
                    Column(Modifier.weight(1f)) {
                        Label(
                            r.items.joinToString(" → ") { it.name },
                            size = 14,
                            weight = FontWeight.Medium,
                        )
                        Label(
                            "${timeText(r.seconds.toDouble())}" +
                                if (r.complete) "" else " · หยุดก่อนจบ",
                            size = 12,
                            color = C.muted,
                        )
                    }
                    if (r.items.any { a.find(it.pid) != null })
                        Pill("Repeat") { a.repeatSession(r) }
                }
            }
        }
        val monthly = a.history.filter { YearMonth.from(date(it)) == ym }
        Row(
            Modifier.fillMaxWidth()
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(C.surface)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(
                    monthly.map { date(it) }.distinct().size.toString() to "days this month",
                    monthly.size.toString() to "sessions",
                    (monthly.sumOf { it.seconds } / 60.0).roundToInt().toString() to "minutes",
                )
                .forEach { (value, caption) ->
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Label(value, size = 22, display = true)
                        Label(caption, size = 11.5, color = C.muted)
                    }
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDialog(a: AppState) {
    val maxHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * .88f
    val blue = Color(if (C.bg == Dark.bg) 0xFF4F86D6 else 0xFF2F64AD)
    ModalBottomSheet(
        onDismissRequest = { if (a.profiles.isNotEmpty()) a.profileOpen = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = C.bg,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = null,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Title("ผู้ใช้", Modifier.weight(1f))
                Pill("ปิด") { if (a.profiles.isNotEmpty()) a.profileOpen = false }
            }
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(C.surface)
                    .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Label(
                    if (a.auth.hasSession()) a.auth.accountEmail()
                    else "ยังไม่ได้ล็อกอิน · ข้อมูลอยู่ในเครื่องนี้",
                    Modifier.weight(1f),
                    size = 13,
                    color = C.muted,
                )
                TextButton(
                    onClick = {
                        a.profileOpen = false
                        a.logout()
                    }
                ) {
                    Label(
                        if (a.auth.hasSession()) "ออกจากระบบ" else "เข้าสู่ระบบ",
                        size = 13,
                        color = blue,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                a.profiles.forEachIndexed { index, profile ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(C.surface)
                            .then(
                                if (profile.id == a.currentProfile)
                                    Modifier.border(2.dp, blue, RoundedCornerShape(16.dp))
                                else Modifier
                            )
                            .clickable { a.switchProfile(profile.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ProfileAvatar(profile, colorIndex = index)
                        Column(Modifier.weight(1f)) {
                            Label(profile.name, weight = FontWeight.Medium)
                            Label(
                                "${profile.age} ปี · ${when(profile.type){"kid"->"Kid"
"care"->"Care"
else->"Adult"}} · ${profile.id}${if(profile.cloud.isNotBlank()) " · ออนไลน์" else ""}",
                                size = 12.5,
                                color = C.muted,
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    Action("แก้ไขข้อมูล", primary = false) { a.editProfile(a.user) }
                }
                Box(Modifier.weight(1f)) {
                    Action("+ เพิ่มผู้ใช้ใหม่", primary = false) { a.editProfile(null) }
                }
            }
        }
    }
}
