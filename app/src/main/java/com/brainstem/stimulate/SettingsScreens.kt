@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.brainstem.stimulate

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(a: AppState) {
    ScreenColumn("settings") {
        Note("Mode")
        CardBox {
            Segments(listOf("Easy", "Pro"), if (a.pro) "Pro" else "Easy") {
                a.pro = it == "Pro"
                a.save()
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("Level 1–10", "Duty cycle", "สร้าง / แก้โปรแกรม", "Helmet spec").forEach {
                    Label(
                        it,
                        Modifier.clip(RoundedCornerShape(10.dp))
                            .background(C.surface2)
                            .padding(horizontal = 9.dp, vertical = 6.dp),
                        size = 12,
                        color = if (a.pro) C.ink else C.muted,
                    )
                }
            }
            Note(
                if (a.pro) "เปิดอยู่ · เห็นใน Quick Start และหน้าโปรแกรม"
                else "Easy ล็อก Level 1 · duty ค่าเริ่มต้น"
            )
        }
        Note("Simulation")
        CardBox {
            Segments(
                listOf("Demo ×20", "Real time"),
                if (a.speed == 20) "Demo ×20" else "Real time",
            ) {
                a.speed = if (it == "Demo ×20") 20 else 1
                a.save()
            }
            Note("เครื่องต้นแบบ · จำลองหมวกและ EEG · ยังไม่ได้เชื่อมต่อฮาร์ดแวร์")
        }
        if (a.pro) {
            Note("Helmet")
            CardBox {
                NumberSetting("Wavelength (nm)", a.spec.wavelength, 600..1100) {
                    a.spec = a.spec.copy(wavelength = it)
                    a.save()
                }
                NumberSetting("Irradiance at 100% (mW/cm²)", a.spec.irradiance, 1..200) {
                    a.spec = a.spec.copy(irradiance = it)
                    a.save()
                }
                NumberSetting("Default duty (%)", a.spec.duty, 10..90, 10) {
                    a.spec = a.spec.copy(duty = it)
                    a.save()
                }
                NumberSetting("Daily max (min)", a.spec.dailyMax, 5..120) {
                    a.spec = a.spec.copy(dailyMax = it)
                    a.save()
                }
                Note("ใช้คำนวณปริมาณแสง (J/cm²)")
            }
            CardBox {
                Expandable("Developer · ESP32 commands & references") {
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
        Note("Appearance")
        CardBox {
            Segments(
                listOf("System", "Light", "Dark"),
                a.theme.replaceFirstChar { it.uppercase() },
            ) {
                a.theme = it.lowercase()
                a.save()
            }
        }
        Note("Account")
        CardBox {
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
private fun NumberSetting(
    label: String,
    value: Int,
    range: IntRange,
    step: Int = 1,
    onChange: (Int) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    var error by remember { mutableStateOf(false) }
    OutlinedTextField(
        text,
        { v ->
            text = v
            val n = v.toIntOrNull()
            error = n == null || n !in range || n % step != 0
            if (!error) onChange(n!!)
        },
        Modifier.fillMaxWidth(),
        label = { Note(label) },
        singleLine = true,
        isError = error,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
    )
    if (error)
        Label(
            "${range.first}–${range.last}" + if (step > 1) " · ขั้นละ $step" else "",
            size = 11,
            color = C.accent,
        )
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
                size = 16,
            )
            RoundAction("›", "เดือนถัดไป") { month = ym.plusMonths(1).toString() }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth()) {
                listOf("อา", "จ", "อ", "พ", "พฤ", "ศ", "ส").forEach {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Note(it) }
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
                            verticalArrangement = Arrangement.Center,
                        ) {
                            if (d > 0) {
                                Label(
                                    d.toString(),
                                    size = 14,
                                    color = if (dt == today) C.accent else C.ink,
                                )
                                if (has)
                                    Box(Modifier.size(5.dp).clip(CircleShape).background(C.accent))
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
            CardBox(padding = 12) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Label(
                        Instant.ofEpochMilli(r.date)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("HH:mm")),
                        size = 12,
                        color = C.accent,
                        mono = true,
                    )
                    Column(Modifier.weight(1f)) {
                        Label(r.items.joinToString(" → ") { it.name }, size = 13)
                        Note(
                            "${timeText(r.seconds.toDouble())}" +
                                if (r.complete) "" else " · หยุดก่อนจบ"
                        )
                    }
                    if (r.items.any { a.find(it.pid) != null })
                        Pill("Repeat") { a.repeatSession(r) }
                }
            }
        }
        val monthly = a.history.filter { YearMonth.from(date(it)) == ym }
        StatTiles(
            listOf(
                monthly.map { date(it) }.distinct().size.toString() to "days this month",
                monthly.size.toString() to "sessions",
                (monthly.sumOf { it.seconds } / 60.0).roundToInt().toString() to "minutes",
            )
        )
    }
}

@Composable
fun ProfileDialog(a: AppState) {
    var adding by remember { mutableStateOf(a.profiles.isEmpty()) }
    var name by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (a.profiles.isNotEmpty()) a.profileOpen = false },
        title = { Title("Who’s using the helmet?", size = 19) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                a.profiles.forEach { profile ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (profile.id == a.currentProfile) C.accent.copy(alpha = .08f)
                                else C.surface2
                            )
                            .clickable { a.switchProfile(profile.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF3D6FB4)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Label(profile.name.take(1), color = Color.White)
                        }
                        Column(Modifier.weight(1f)) {
                            Label(profile.name, size = 14)
                            Note("${profile.age} ปี")
                        }
                        if (profile.id == a.currentProfile)
                            Label("กำลังใช้", size = 10, color = C.ok)
                    }
                }
                if (!adding)
                    TextButton(onClick = { adding = true }) {
                        Label("+ Add User", color = C.accent)
                    }
                if (adding) {
                    InputField(name, { name = it }, "ชื่อที่แสดง")
                    InputField(age, { age = it }, "อายุ (ปี)", KeyboardType.Number)
                    Note("โปรไฟล์ใหม่นี้เก็บในเครื่อง")
                }
            }
        },
        confirmButton = {
            if (adding)
                TextButton(
                    enabled = name.isNotBlank() && age.toIntOrNull() in 3..110,
                    onClick = { a.addLocalProfile(name, age.toInt()) },
                ) {
                    Label("บันทึกและใช้งาน", color = C.accent)
                }
        },
        dismissButton = {
            if (a.profiles.isNotEmpty())
                TextButton(onClick = { a.profileOpen = false }) { Label("ปิด") }
        },
    )
}
