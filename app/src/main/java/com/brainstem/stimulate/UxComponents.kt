package com.brainstem.stimulate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SessionContextBar(a: AppState, modifier: Modifier = Modifier, allowSwitch: Boolean = true) {
    val record = a.summary.takeIf { a.screen == Screen.SUMMARY && a.engine == null }
    val speed = record?.simulationSpeed ?: a.speed
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.weight(1f)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(C.surface)
                .clickable(enabled = allowSwitch && a.draft == null, role = Role.Button) {
                    a.profileOpen = true
                }
                .semantics { contentDescription = "โปรไฟล์ที่ใช้งาน ${a.user?.name.orEmpty()}" }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            a.user?.let { ProfileAvatar(it, Modifier.size(28.dp)) }
            Column(Modifier.weight(1f)) {
                Text(
                    a.user?.name ?: "Choose profile",
                    fontFamily = BodyFont,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = C.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Label(
                    if (a.guestMode) "Guest · เก็บในเครื่องนี้" else "โปรไฟล์ที่ใช้งาน",
                    size = 11,
                    color = C.muted,
                )
            }
            if (allowSwitch && a.draft == null) Label("⌄", color = C.muted)
        }
        Label(
            if (speed > 0) "Simulation · ${speed}×" else "Simulation · saved",
            Modifier.clip(RoundedCornerShape(12.dp))
                .background(C.surface2)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            size = 12,
            weight = FontWeight.Medium,
            color = C.ink,
        )
    }
}

@Composable
fun DraftExitDialog(a: AppState) {
    AlertDialog(
        onDismissRequest = { a.keepEditingDraft() },
        containerColor = C.surface,
        title = { Title("Save changes?", size = 22) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Label("มีการแก้ไขที่ยังไม่ได้บันทึก เลือกบันทึก ทิ้งการแก้ไข หรือแก้ไขต่อ")
                if (a.error.isNotBlank()) Label(a.error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { a.discardDraftAndLeave() }) {
                    Label("Discard changes", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { a.saveDraftAndLeave() }) { Label("Save changes") }
        },
        dismissButton = { TextButton(onClick = { a.keepEditingDraft() }) { Label("Keep editing") } },
    )
}

@Composable
fun SaveProgramDialog(initialName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initialName.take(40)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.surface,
        title = { Title("Save program", size = 22) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Label("Program name", weight = FontWeight.Medium)
                InputField(name, { name = it.take(40) }, "ตั้งชื่อเพื่อหาโปรแกรมนี้ได้ง่าย")
                Note("บันทึกไว้ใน My Programs ของเครื่องนี้")
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) {
                Label("Save program")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Label("Cancel") } },
    )
}
