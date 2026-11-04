package com.brainstem.stimulate

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Local emulator smoke test. Never signs into, creates, or modifies a real online account. */
class StimulateFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val state
        get() = ViewModelProvider(rule.activity)[AppState::class.java]

    private fun discardIfAsked() {
        if (rule.onAllNodesWithText("Discard changes").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("Discard changes").performClick()
        }
    }

    private fun snapshot(name: String) {
        rule.waitForIdle()
        // Platform ripples animate on the render thread, outside the Compose test clock.
        Thread.sleep(350)
        val inst = InstrumentationRegistry.getInstrumentation()
        val root = if (state.profileFormOpen) rule.onNode(isDialog()) else rule.onRoot()
        val bitmap = root.captureToImage().asAndroidBitmap()
        val output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val file =
            if (output != null) File(output, "$name.png")
            else File(inst.targetContext.getExternalFilesDir(null), "screenshots/$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val layouts = org.json.JSONArray()
        rule
            .onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.Text),
                useUnmergedTree = true,
            )
            .fetchSemanticsNodes()
            .forEach { node ->
                val bounds = node.boundsInRoot
                layouts.put(
                    org.json
                        .JSONObject()
                        .put("text", node.config[SemanticsProperties.Text].joinToString())
                        .put("x", bounds.left)
                        .put("y", bounds.top)
                        .put("width", bounds.width)
                        .put("height", bounds.height)
                )
            }
        File(file.parentFile, "$name.layout.json").writeText(layouts.toString(2))
    }

    private fun finishHeadsetCheck() {
        var lastAction = 0L
        rule.waitUntil(20000) {
            rule.mainClock.advanceTimeBy(100)
            if (!state.closedLoop && android.os.SystemClock.elapsedRealtime() - lastAction >= 500) {
                val action =
                    listOf("สัญญาณดี · เปิด Closed-loop", "ปรับแล้ว", "แก้แล้ว").firstOrNull { label
                        ->
                        rule.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
                    }
                if (action != null) {
                    rule.onNodeWithText(action).performScrollTo().performClick()
                    lastAction = android.os.SystemClock.elapsedRealtime()
                }
            }
            state.closedLoop
        }
    }

    @Test
    fun stimulateEndToEnd() {
        snapshot("01-login")
        rule.runOnIdle {
            state.guest()
            state.engine = null
            state.queue.clear()
            state.history.clear()
            state.customs.clear()
            state.favorites.clear()
            state.pro = false
            state.speed = 20
            state.spec = Spec()
            state.closedLoop = false
            state.theme = "light"
            state.screen = Screen.HOME
            state.category = "relax"
            state.quick = Step()
            state.quickMinutes = 10
        }
        rule.onNodeWithText("Today’s Session").assertIsDisplayed()
        rule.onNodeWithText("Train").assertDoesNotExist()
        rule.onNodeWithText("Map").assertDoesNotExist()
        snapshot("02-stimulate")
        rule.onNodeWithContentDescription("คำอธิบาย Alpha Calm").performClick().assertIsSelected()
        rule.runOnIdle { assertEquals(Screen.HOME, state.screen) }
        rule
            .onNodeWithText(
                state.repo.presets.first { it.id == "std-alpha" }.goal,
                substring = true,
            )
            .assertExists()
        snapshot("20-program-info-expanded")
        rule
            .onNodeWithContentDescription("คำอธิบาย Alpha Calm")
            .performClick()
            .assertIsNotSelected()
        rule.runOnIdle { assertEquals(Screen.HOME, state.screen) }
        rule.onNodeWithText("Focus", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Gamma Focus").assertIsDisplayed()
        rule.onNodeWithText("Learn", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Memory & Learning").assertIsDisplayed()
        rule.onNodeWithText("Relax", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Alpha Calm").performClick()
        rule.onNodeWithContentDescription("ตั้งค่า").assertIsDisplayed()
        rule.onNodeWithContentDescription("ติดดาว").performClick()
        snapshot("03-program")
        rule.runOnIdle { state.pro = true }
        val duplicate = rule.onNodeWithText("Duplicate & Edit").performScrollTo()
        val duplicateBounds = duplicate.fetchSemanticsNode().boundsInRoot
        val density = rule.activity.resources.displayMetrics.density
        assertEquals(16 * density, duplicateBounds.left, density)
        assertEquals(
            rule.onRoot().fetchSemanticsNode().boundsInRoot.width - 32 * density,
            duplicateBounds.width,
            density,
        )
        assertEquals(36.15f * density, duplicateBounds.height, density)
        rule.onNodeWithText("Start  10 min").assertIsDisplayed()
        snapshot("22-program-actions")
        duplicate.performClick()
        rule.onNodeWithText("Alpha Calm (copy)").assertIsDisplayed()
        rule.runOnIdle {
            assertEquals("custom", state.draft!!.src)
            assertEquals("custom", state.draft!!.cat)
            assertNotEquals(state.selected!!.id, state.draft!!.id)
            assertEquals(state.selected!!.steps, state.draft!!.steps)
            assertTrue("Opening a copy must not save it", state.customs.isEmpty())
        }
        rule.onNodeWithContentDescription("ขึ้น").assertIsNotEnabled()
        rule.onNodeWithContentDescription("ลง").assertIsNotEnabled()
        rule.onNodeWithContentDescription("ลบ").assertIsNotEnabled()
        rule.onNodeWithContentDescription("ทำซ้ำ").assertIsEnabled()
        snapshot("21-builder-alpha-copy")
        rule.onNodeWithText("Whole head").assertIsSelected()
        rule.onNodeWithText("Custom…").assertIsNotSelected().performClick().assertIsSelected()
        listOf("Whole head", "Front", "Back", "Limbic").forEach {
            rule.onNodeWithText(it).assertIsNotSelected()
        }
        rule.runOnIdle { assertEquals(POINTS, state.draft!!.steps.first().pts) }
        rule.onNodeWithContentDescription("Fp1").performClick()
        rule.runOnIdle { assertFalse("Fp1" in state.draft!!.steps.first().pts) }
        rule.onNodeWithContentDescription("Fp1").performClick()
        rule.runOnIdle { assertEquals(POINTS, state.draft!!.steps.first().pts) }
        rule.onNodeWithText("Custom…").assertIsSelected()
        rule.onNodeWithText("Whole head").assertIsNotSelected()
        snapshot("24-builder-custom-area")
        rule.onNodeWithText("Front").performClick().assertIsSelected()
        rule.onNodeWithText("Custom…").assertIsNotSelected()
        rule.runOnIdle { assertEquals(AREAS.getValue("Front"), state.draft!!.steps.first().pts) }
        rule.onNodeWithText("Whole head").performClick().assertIsSelected()
        rule.onNodeWithText("Custom…").assertIsNotSelected()
        rule.onNodeWithContentDescription("ทำซ้ำ").performClick()
        rule.runOnIdle { assertEquals(2, state.draft!!.steps.size) }
        rule.onNodeWithContentDescription("ลบ").assertIsEnabled().performClick()
        rule.runOnIdle { assertEquals(1, state.draft!!.steps.size) }
        rule.onNodeWithContentDescription("ลบ").assertIsNotEnabled()
        rule.onNodeWithText("Cancel").performScrollTo().performClick()
        discardIfAsked()
        rule.runOnIdle {
            state.pro = false
            assertEquals(Screen.PROGRAM, state.screen)
        }
        rule.onNodeWithText("+ Session").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals("std-alpha", state.queue.single().pid) }
        snapshot("04-session-queue")
        // The confirmation Snackbar temporarily covers lower scrolling content.
        rule.waitUntil(6000) { state.notice.isEmpty() }
        rule.onNodeWithText("Quick Start").performClick()
        rule.onNodeWithContentDescription("ตั้งค่า").assertIsDisplayed()
        rule.onNodeWithText("Clear", useUnmergedTree = true).performClick()
        rule.onNodeWithText("เลือกจุดก่อน").assertIsDisplayed()
        rule.onNodeWithContentDescription("F3").performClick()
        rule.runOnIdle { assertEquals(listOf("F3"), state.quick.pts) }
        rule.onNodeWithContentDescription("F3").performClick()
        rule.runOnIdle { assertTrue(state.quick.pts.isEmpty()) }
        rule.onNodeWithText("All", useUnmergedTree = true).performClick()
        snapshot("05-quick-start")
        rule.onNodeWithContentDescription("เพิ่มความถี่").performScrollTo()
        rule.onNodeWithText("Sweep", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Broad").performScrollTo().performClick()
        snapshot("06-sweep")
        rule.runOnIdle {
            assertEquals(1.0, state.quick.f1, 0.0)
            assertEquals(45.0, state.quick.f2, 0.0)
        }
        rule.onNodeWithText("Off").performScrollTo().performClick()
        rule.onNodeWithText("Headset check").assertIsDisplayed()
        snapshot("12-headset-check")
        finishHeadsetCheck()
        rule.runOnIdle { assertTrue(state.closedLoop) }
        rule.onNodeWithText("On").performScrollTo().performClick()
        rule.onNodeWithText("Off").performScrollTo().performClick()
        finishHeadsetCheck()
        rule.runOnIdle { assertTrue(state.wasHeadsetCheckedToday()) }
        rule.onNodeWithText("On").performScrollTo().performClick()
        rule.onNodeWithText("Sweep", useUnmergedTree = true).performScrollTo()
        rule.onNodeWithText("Fixed", useUnmergedTree = true).performScrollTo().performClick()
        rule.runOnIdle { state.quickMinutes = 1 }
        rule.onNodeWithText("Start  10 Hz · 1 min").performClick()
        rule.onNodeWithText("Pause").assertIsDisplayed().performClick()
        var pausedAt = 0.0
        rule.runOnIdle {
            assertTrue(state.engine!!.paused)
            pausedAt = state.engine!!.elapsed
        }
        Thread.sleep(500)
        rule.runOnIdle { assertEquals(pausedAt, state.engine!!.elapsed, 0.0) }
        snapshot("07-running-paused")
        rule.onNodeWithText("Resume").assertIsDisplayed().performClick()
        rule.runOnIdle { assertFalse(state.engine!!.paused) }
        rule.waitUntil(15000) { state.engine == null }
        rule.onNodeWithText("Session Complete").assertIsDisplayed()
        rule.runOnIdle {
            assertEquals(60, state.history.single().seconds)
            assertTrue(state.history.single().complete)
        }
        snapshot("08-complete")
        rule.onNodeWithText("Back to Home").performScrollTo().performClick()
        rule.runOnIdle { state.navigate(Screen.HISTORY) }
        rule.onNodeWithText("Quick · 10 Hz", useUnmergedTree = true).assertExists()
        snapshot("09-history")
        rule.runOnIdle {
            state.navigate(Screen.HOME)
            state.pro = true
            state.newProgram()
        }
        snapshot("16-builder-new")
        rule.onNodeWithText("โปรแกรมใหม่").performTextReplacement("Native test program")
        rule.onNodeWithText("Sweep").performScrollTo().performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
        // Scroll the presets below the steppers into view, away from the viewport edge.
        rule.onNodeWithText("Broad").performScrollTo()
        repeat(5) {
            rule.onAllNodesWithContentDescription("เพิ่ม Hz").onFirst().performSemanticsAction(
                SemanticsActions.OnClick
            ) {
                it()
            }
        }
        rule.runOnIdle {
            assertEquals(13.0, state.draft!!.steps.first().f1, 0.0)
            assertEquals(12.0, state.draft!!.steps.first().f2, 0.0)
        }
        // Bring the following row into view so the duration buttons are clear of the screen edge.
        rule.onNodeWithText("Duty cycle").performScrollTo()
        rule.onNodeWithContentDescription("เพิ่ม min").assertIsDisplayed()
        val durationIncrease =
            rule.onNodeWithContentDescription("เพิ่ม min").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Duration button remains full-width on compact displays",
            durationIncrease.width >= 28 * rule.activity.resources.displayMetrics.density,
        )
        rule.onNodeWithContentDescription("เพิ่ม min").performClick()
        rule.runOnIdle { assertEquals(360, state.draft!!.steps.first().dur) }
        rule.onNodeWithContentDescription("ลด min").performScrollTo().performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
        rule.runOnIdle { assertEquals(300, state.draft!!.steps.first().dur) }
        snapshot("17-builder-sweep")
        rule.onNodeWithText("+ Add Step").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(2, state.draft!!.steps.size) }
        rule.onNodeWithContentDescription("ทำซ้ำ").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(3, state.draft!!.steps.size) }
        rule.onNodeWithContentDescription("ขึ้น").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, state.editStep) }
        rule.onNodeWithContentDescription("ลง").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(2, state.editStep) }
        rule.onNodeWithContentDescription("ลบ").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(2, state.draft!!.steps.size) }
        rule.onNodeWithText("Save Program").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(2, state.customs.single().steps.size)
            assertEquals("Native test program", state.customs.single().name)
        }
        snapshot("10-custom-program")
        rule.waitUntil(6000) { state.notice.isEmpty() }
        rule.onNodeWithText("Duplicate").performScrollTo()
        rule.onNodeWithText("Edit", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Delete", useUnmergedTree = true).assertExists()
        snapshot("23-custom-program-actions")
        rule.onNodeWithText("Duplicate").performClick()
        rule.runOnIdle {
            assertEquals("Native test program (copy)", state.draft!!.name)
            assertNotEquals(state.selected!!.id, state.draft!!.id)
            assertEquals(state.selected!!.steps, state.draft!!.steps)
            assertEquals(1, state.customs.size)
        }
        rule.onNodeWithText("Cancel").performScrollTo().performClick()
        discardIfAsked()
        rule.onNodeWithText("Edit", useUnmergedTree = true).performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(state.selected!!.id, state.draft!!.id)
            assertEquals("Native test program", state.draft!!.name)
        }
        rule.onNodeWithText("Cancel").performScrollTo().performClick()
        discardIfAsked()
        rule.runOnIdle { state.navigate(Screen.SETTINGS) }
        snapshot("18-settings-pro")
        rule.runOnIdle { state.pro = false }
        snapshot("19-settings-easy")
        rule.runOnIdle {
            state.pro = true
            state.theme = "dark"
        }
        snapshot("11-dark-settings")
        rule.runOnIdle {
            val record = state.history.single()
            val older =
                record.copy(
                    date = record.date - 10000,
                    items = listOf(SessionItem("std-alpha", "Alpha Calm", 10, 600)),
                )
            state.history.clear()
            state.history.add(older)
            repeat(4) {
                state.history.add(
                    record.copy(
                        date = record.date + it,
                        items = listOf(SessionItem("quick", "Quick · 10 Hz", it + 1, 60)),
                    )
                )
            }
            assertEquals(listOf("std-alpha"), state.recentSessions().single().items.map { it.pid })
        }
        rule.runOnIdle {
            state.theme = "light"
            state.editProfile(null)
            state.profileOpen = true
        }
        rule.onNodeWithText("เริ่มใช้งาน").performScrollTo().performClick()
        rule.onNodeWithText("กรอกชื่อจริง").assertIsDisplayed()
        rule.onNodeWithText("ยกเลิก").performScrollTo().performClick()
        var originalProfile = ""
        rule.runOnIdle {
            originalProfile = state.currentProfile
            state.editProfile(
                Profile(
                    "P-TEST",
                    "Profile Test",
                    age = 9,
                    realName = "Profile Test Person",
                    sex = "female",
                    birth = "2017-01-02",
                    hand = "left",
                    guardianName = "Parent",
                    guardianRelation = "แม่",
                    guardianPhone = "0812345678",
                )
            )
        }
        rule.onNodeWithText("Profile Test").performTextReplacement("Updated profile")
        rule.onNodeWithText("แก้ไขข้อมูล").assertIsDisplayed()
        snapshot("13-profile-form")
        rule.onNodeWithText("บันทึก").performScrollTo().performClick()
        rule.waitUntil(10000) { !state.profileFormOpen }
        rule.runOnIdle {
            val saved =
                profileFrom(
                    state.repo.read("data.guest").getJSONArray("profiles").objects().first {
                        it.getString("id") == "P-TEST"
                    }
                )
            assertEquals("Updated profile", saved.name)
            assertEquals("2017-01-02", saved.birth)
            assertEquals("female", saved.sex)
            assertEquals("left", saved.hand)
            assertEquals("0812345678", saved.guardianPhone)
            state.switchProfile(originalProfile)
            state.profiles.removeAll { it.id == "P-TEST" }
            state.closedLoop = true
            state.start(
                listOf(
                    Program("closed-loop-test", "Closed-loop test", steps = listOf(Step(dur = 10)))
                )
            )
            state.pause()
        }
        rule.onNodeWithText("หลับตา").assertIsDisplayed()
        snapshot("14-closed-loop-baseline")
        rule.onNodeWithText("Resume").assertIsDisplayed().performClick()
        rule.runOnIdle { assertFalse(state.engine!!.paused) }
        rule.waitUntil(15000) { state.engine == null }
        rule.runOnIdle {
            val last = state.history.last()
            assertTrue(last.closedLoop)
            assertTrue(last.iapf in 9.3..10.5)
            assertEquals(1, last.alphaTrace.size)
            assertEquals(4, last.deltas.size)
            assertTrue(last.events.isNotEmpty())
        }
        snapshot("15-closed-loop-summary")
        rule.runOnIdle {
            state.navigate(Screen.HOME)
            state.category = "relax"
            state.pro = false
            state.queue.clear()
            state.history.clear()
            state.customs.clear()
            state.favorites.clear()
            state.save()
        }
    }
}
