package com.brainstem.stimulate

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
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

    private fun snapshot(name: String) {
        rule.waitForIdle()
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
        rule.onNodeWithText("Focus", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Gamma Focus").assertIsDisplayed()
        rule.onNodeWithText("Learn", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Memory & Learning").assertIsDisplayed()
        rule.onNodeWithText("Relax", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Alpha Calm").performClick()
        rule.onNodeWithContentDescription("ตั้งค่า").assertIsDisplayed()
        rule.onNodeWithContentDescription("ติดดาว").performClick()
        snapshot("03-program")
        rule.onNodeWithText("+ Session").performScrollTo().performClick()
        rule.runOnIdle { assertEquals("std-alpha", state.queue.single().pid) }
        snapshot("04-session-queue")
        rule.onNodeWithText("Quick Start").performClick()
        rule.onNodeWithContentDescription("ตั้งค่า").assertIsDisplayed()
        rule.onNodeWithText("Clear", useUnmergedTree = true).performClick()
        rule.onNodeWithText("เลือกจุดก่อน").assertIsDisplayed()
        rule.onNodeWithText("All", useUnmergedTree = true).performClick()
        snapshot("05-quick-start")
        rule.onNodeWithText("Sweep", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Broad", useUnmergedTree = true).performClick()
        rule.runOnIdle {
            assertEquals(1.0, state.quick.f1, 0.0)
            assertEquals(45.0, state.quick.f2, 0.0)
        }
        snapshot("06-sweep")
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
        rule.onNodeWithText("Fixed", useUnmergedTree = true).performClick()
        rule.runOnIdle { state.quickMinutes = 1 }
        rule.onNodeWithText("Start  10 Hz · 1 min").performClick()
        rule.onNodeWithText("Pause").performClick()
        var pausedAt = 0.0
        rule.runOnIdle {
            assertTrue(state.engine!!.paused)
            pausedAt = state.engine!!.elapsed
        }
        Thread.sleep(500)
        rule.runOnIdle { assertEquals(pausedAt, state.engine!!.elapsed, 0.0) }
        snapshot("07-running-paused")
        rule.onNodeWithText("Resume").performClick()
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
        rule.onNodeWithText("โปรแกรมใหม่").performTextReplacement("Native test program")
        rule.onNodeWithText("Sweep", useUnmergedTree = true).performScrollTo().performClick()
        repeat(5) {
            rule
                .onAllNodesWithContentDescription("เพิ่ม Hz")
                .onFirst()
                .performScrollTo()
                .performClick()
        }
        rule.runOnIdle {
            assertEquals(13.0, state.draft!!.steps.first().f1, 0.0)
            assertEquals(12.0, state.draft!!.steps.first().f2, 0.0)
        }
        rule.onNodeWithText("+ Add Step").performScrollTo().performClick()
        rule.onNodeWithText("Save Program").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(2, state.customs.single().steps.size)
            assertEquals("Native test program", state.customs.single().name)
        }
        snapshot("10-custom-program")
        rule.runOnIdle {
            state.navigate(Screen.SETTINGS)
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
        rule.onNodeWithText("Resume").performClick()
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
