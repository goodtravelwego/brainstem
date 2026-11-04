package com.brainstem.stimulate

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.Period

private fun profilePhoto(bitmap: Bitmap): String {
    val side = minOf(bitmap.width, bitmap.height)
    val crop =
        Bitmap.createBitmap(
            bitmap,
            (bitmap.width - side) / 2,
            (bitmap.height - side) / 2,
            side,
            side,
        )
    val scaled = crop.scale(128, 128)
    return ByteArrayOutputStream().use { output ->
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, output)
        Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }
}

@Composable
fun ProfileAvatar(profile: Profile, modifier: Modifier = Modifier, colorIndex: Int = 0) {
    val image =
        remember(profile.photo) {
            runCatching {
                    Base64.decode(profile.photo, Base64.DEFAULT).let {
                        BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
                    }
                }
                .getOrNull()
        }
    val colors = listOf(0xFF2F64AD, 0xFFD9533F, 0xFF6E4FC0, 0xFF1F8277, 0xFFB07A10, 0xFFB5487A)
    val color =
        Color(colors[if (profile.id == "new") colors.lastIndex else colorIndex.mod(colors.size)])
    Box(
        modifier.size(30.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) Image(image, profile.name, Modifier.fillMaxSize())
        else
            Canvas(Modifier.fillMaxSize()) {
                val unit = size.width / 60
                val kid = profile.type == "kid"
                drawCircle(
                    Color.White.copy(alpha = .9f),
                    (if (kid) 11f else 10f) * unit,
                    Offset(30 * unit, (if (kid) 25f else 24f) * unit),
                )
                val path =
                    Path().apply {
                        moveTo((if (kid) 13f else 11f) * unit, 60 * unit)
                        relativeCubicTo(0f, -11 * unit, 8 * unit, -19 * unit, 17 * unit, -19 * unit)
                        relativeCubicTo(9 * unit, 0f, 17 * unit, 8 * unit, 17 * unit, 19 * unit)
                        close()
                    }
                drawPath(path, Color.White.copy(alpha = .9f))
            }
    }
}

@Composable
fun ProfileForm(a: AppState) {
    val old = a.profileEditing
    val first = a.profiles.isEmpty()
    val signed = a.auth.hasSession()
    var real by rememberSaveable(old?.id) { mutableStateOf(old?.realName.orEmpty()) }
    var nick by rememberSaveable(old?.id) { mutableStateOf(old?.name.orEmpty()) }
    var birth by rememberSaveable(old?.id) { mutableStateOf(old?.birth.orEmpty()) }
    var sex by rememberSaveable(old?.id) { mutableStateOf(old?.sex.orEmpty()) }
    var type by rememberSaveable(old?.id) { mutableStateOf(old?.type ?: "kid") }
    var typeSet by rememberSaveable(old?.id) { mutableStateOf(old != null) }
    var hand by rememberSaveable(old?.id) { mutableStateOf(old?.hand.orEmpty()) }
    var guardian by rememberSaveable(old?.id) { mutableStateOf(old?.guardianName.orEmpty()) }
    var relation by rememberSaveable(old?.id) { mutableStateOf(old?.guardianRelation.orEmpty()) }
    var phone by rememberSaveable(old?.id) { mutableStateOf(old?.guardianPhone.orEmpty()) }
    var photo by rememberSaveable(old?.id) { mutableStateOf(old?.photo.orEmpty()) }
    var consent by rememberSaveable { mutableStateOf(false) }
    var dateOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val camera =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { image ->
            if (image != null) photo = profilePhoto(image)
        }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null)
                runCatching {
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        context.contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it, null, options)
                        }
                        var sample = 1
                        while (
                            options.outWidth / sample > 1024 || options.outHeight / sample > 1024
                        ) sample *= 2
                        options.inJustDecodeBounds = false
                        options.inSampleSize = sample
                        context.contentResolver
                            .openInputStream(uri)
                            ?.use { BitmapFactory.decodeStream(it, null, options) }
                            ?.let { photo = profilePhoto(it) }
                    }
                    .onFailure { a.error = "เปิดรูปไม่สำเร็จ · ลองเลือกรูปอีกครั้ง" }
        }
    val age =
        runCatching { Period.between(LocalDate.parse(birth), LocalDate.now()).years }.getOrNull()
    fun close() {
        if (!first && !a.busy) {
            a.profileFormOpen = false
            a.profileEditing = null
            a.error = ""
        }
    }
    Dialog(
        onDismissRequest = { close() },
        properties =
            DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            Modifier.fillMaxSize()
                .background(C.bg)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (first) Wordmark(30, activeModule = false)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Title(
                    if (first) "สร้างโปรไฟล์"
                    else if (old == null) "เพิ่มผู้ใช้" else "แก้ไขข้อมูล",
                    Modifier.weight(1f),
                    size = 24,
                )
                if (!first) TextButton(onClick = { close() }, enabled = !a.busy) { Label("ยกเลิก") }
            }
            if (first) Note("กรอกข้อมูลของผู้ที่จะใช้หมวก (เช่น ลูก) · แก้ไขทีหลังได้")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProfileAvatar(
                    Profile("new", nick, type = type, photo = photo),
                    Modifier.size(64.dp),
                )
                Column {
                    Row {
                        TextButton(
                            onClick = {
                                runCatching { camera.launch(null) }
                                    .onFailure { a.error = "ไม่พบแอปกล้อง" }
                            }
                        ) {
                            Label("ถ่ายรูป", size = 13)
                        }
                        TextButton(onClick = { picker.launch("image/*") }) {
                            Label("เลือกรูป", size = 13)
                        }
                    }
                    if (photo.isNotBlank())
                        TextButton(onClick = { photo = "" }) { Note("ไม่ใช้รูป") }
                }
            }
            ProfileField("ชื่อจริง - นามสกุล *") { InputField(real, { real = it.take(120) }, "") }
            ProfileField("ชื่อเล่น (ชื่อที่แสดงในแอป)") {
                InputField(nick, { nick = it.take(60) }, "เช่น น้องต้นกล้า")
            }
            ProfileField("เพศ") {
                ProfileSegments(
                    sex,
                    listOf("male" to "ชาย", "female" to "หญิง", "unspecified" to "ไม่ระบุ"),
                ) {
                    sex = it
                }
            }
            ProfileField("วันเดือนปีเกิด *") {
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(C.surface)
                        .border(1.dp, C.line, RoundedCornerShape(12.dp))
                        .clickable { dateOpen = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Label(birth.ifBlank { "เลือกวันเกิด" }, size = 14)
                }
                if (age != null) {
                    val period = Period.between(LocalDate.parse(birth), LocalDate.now())
                    val formatted =
                        java.time.chrono.ThaiBuddhistDate.from(LocalDate.parse(birth))
                            .format(
                                java.time.format.DateTimeFormatter.ofPattern(
                                    "d MMMM yyyy",
                                    java.util.Locale.forLanguageTag("th-TH"),
                                )
                            )
                    Note(
                        "อายุ $age ปี${if(period.months>0) " ${period.months} เดือน" else ""} · เกิด $formatted"
                    )
                }
            }
            ProfileField("กลุ่มผู้ใช้") {
                ProfileSegments(
                    type,
                    listOf("kid" to "เด็ก", "adult" to "ผู้ใหญ่", "care" to "ผู้สูงวัย"),
                ) {
                    type = it
                    typeSet = true
                }
            }
            ProfileField("ถนัดมือ") {
                ProfileSegments(
                    hand,
                    listOf("right" to "ขวา", "left" to "ซ้าย", "both" to "ทั้งสองมือ"),
                ) {
                    hand = it
                }
                Note("ใช้กับเกมกดปุ่ม และการแปลผลสมองซีกซ้าย-ขวา")
            }
            if (type != "adult" || age != null && age < 18)
                CardBox(padding = 14) {
                    Label("ผู้ปกครอง / ผู้ดูแล")
                    InputField(guardian, { guardian = it.take(120) }, "ชื่อผู้ปกครอง")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            InputField(relation, { relation = it.take(40) }, "เกี่ยวข้องเป็น")
                        }
                        Box(Modifier.weight(1.2f)) {
                            InputField(
                                phone,
                                { phone = it.take(30) },
                                "เบอร์โทร",
                                keyboard = KeyboardType.Phone,
                            )
                        }
                    }
                }
            if (signed && old?.cloud.isNullOrBlank())
                Row(verticalAlignment = Alignment.Top) {
                    Checkbox(consent, { consent = it })
                    Label(
                        "ยินยอมให้เก็บข้อมูลนี้ออนไลน์ เพื่อใช้ในโปรแกรมฝึกสมอง · เห็นได้เฉพาะบัญชีนี้ · ขอลบได้ทุกเมื่อ",
                        Modifier.weight(1f),
                        size = 12,
                    )
                }
            if (!signed) Note("ไม่ได้ล็อกอิน: ข้อมูลเก็บในเครื่องนี้เท่านั้น")
            if (a.error.isNotBlank()) Label(a.error, size = 12, color = C.accent)
            Action(
                if (a.busy) "กำลังบันทึก…" else if (old == null) "เริ่มใช้งาน" else "บันทึก",
                enabled = !a.busy,
            ) {
                a.error =
                    when {
                        real.isBlank() -> "กรอกชื่อจริง"
                        age == null || age < 0 || LocalDate.parse(birth).isAfter(LocalDate.now()) ->
                            "เลือกวันเกิด"
                        phone.isNotBlank() && !Regex("^[0-9+\\-\\s()]{6,30}$").matches(phone) ->
                            "เบอร์โทรไม่ถูกต้อง"
                        signed && old?.cloud.isNullOrBlank() && !consent -> "กรุณาติ๊กยินยอมก่อน"
                        else -> ""
                    }
                if (a.error.isBlank())
                    a.saveProfile(
                        Profile(
                            old?.id ?: a.nextProfileId(),
                            nick.trim().ifBlank { real.trim().substringBefore(' ') },
                            age!!.coerceIn(1, 120),
                            old?.cloud.orEmpty(),
                            real.trim(),
                            sex,
                            birth,
                            type,
                            hand,
                            guardian.trim(),
                            relation.trim(),
                            phone.trim(),
                            photo,
                            old?.photoPath.orEmpty(),
                        )
                    )
            }
            if (first)
                TextButton(
                    onClick = {
                        a.profileFormOpen = false
                        a.profileOpen = false
                        a.logout()
                    }
                ) {
                    Label("ออกจากระบบ")
                }
        }
    }
    if (dateOpen) {
        val selected =
            runCatching { LocalDate.parse(birth) }.getOrDefault(LocalDate.now().minusYears(9))
        DisposableEffect(Unit) {
            val dialog =
                android.app.DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        birth = LocalDate.of(y, m + 1, d).toString()
                        if (!typeSet)
                            type =
                                if (
                                    Period.between(LocalDate.parse(birth), LocalDate.now()).years <
                                        18
                                )
                                    "kid"
                                else "adult"
                        dateOpen = false
                    },
                    selected.year,
                    selected.monthValue - 1,
                    selected.dayOfMonth,
                )
            dialog.datePicker.maxDate = System.currentTimeMillis()
            dialog.datePicker.minDate =
                LocalDate.of(1900, 1, 1)
                    .atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            dialog.setOnDismissListener { dateOpen = false }
            dialog.show()
            onDispose { dialog.dismiss() }
        }
    }
}

@Composable
private fun ProfileField(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Note(label)
        content()
    }
}

@Composable
private fun ProfileSegments(
    value: String,
    choices: List<Pair<String, String>>,
    onChange: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(C.surface2)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        choices.forEach { (id, label) ->
            Box(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (id == value) C.surface else C.surface2)
                    .clickable { onChange(id) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Label(label, size = 13, color = if (id == value) C.accent else C.muted)
            }
        }
    }
}
