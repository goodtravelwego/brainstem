package com.brainstem.stimulate

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UX regressions use only the verification application's local guest data. */
class UxRegressionTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val state
        get() = ViewModelProvider(rule.activity)[AppState::class.java]

    @Before
    fun setUp() {
        rule.runOnIdle {
            state.engine = null
            state.discardDraftAndLeave()
            state.screen = Screen.HOME
            state.guest()
            state.switchProfile(state.profiles.first().id)
            state.screen = Screen.HOME
            state.pro = true
            state.theme = "light"
            state.speed = 20
            state.spec = Spec()
            state.level = 1
            state.battery = 92
            state.closedLoop = false
            state.queue.clear()
            state.history.clear()
            state.customs.clear()
            state.favorites.clear()
            state.quick = Step()
            state.quickMinutes = 10
            state.category = "relax"
            state.notice = ""
            state.error = ""
        }
    }

    @After
    fun cleanUp() {
        rule.runOnIdle {
            state.engine = null
            state.discardDraftAndLeave()
            state.screen = Screen.HOME
            state.notice = ""
            state.save()
        }
    }

    private fun snapshot(name: String, dialog: Boolean = false) {
        rule.waitForIdle()
        Thread.sleep(250)
        val inst = InstrumentationRegistry.getInstrumentation()
        val bitmap =
            (if (dialog) rule.onNode(isDialog()) else rule.onRoot())
                .captureToImage()
                .asAndroidBitmap()
        val output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val directory =
            if (output != null) File(output, "ux")
            else File(inst.targetContext.getExternalFilesDir(null), "screenshots/ux")
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }

    @Test
    fun changedDraftRequiresAChoiceAndCanBeSaved() {
        rule.runOnIdle {
            val program = Program("ux-draft", "Draft protection", steps = listOf(Step(f = 10.0)))
            state.customs.add(program)
            state.open(program)
            state.editProgram(false)
            state.updateStep(state.draft!!.steps.first().copy(f = 11.0))
            state.updateStep(state.draft!!.steps.first().copy(f = 10.0))
            assertFalse(
                "Reverted values do not prompt because of a generated step label",
                state.draftDirty,
            )
            state.updateStep(state.draft!!.steps.first().copy(f = 11.0))
        }
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithText("Save changes?").assertIsDisplayed()
        snapshot("03-unsaved-changes", dialog = true)
        rule.onNodeWithText("Keep editing").performClick()
        rule.runOnIdle {
            assertEquals(Screen.BUILDER, state.screen)
            assertEquals(11.0, state.draft!!.steps.first().f, 0.0)
            state.back()
        }
        rule.onNodeWithText("Discard changes").performClick()
        rule.runOnIdle {
            assertEquals(Screen.PROGRAM, state.screen)
            assertEquals(10.0, state.customs.single().steps.first().f, 0.0)
            state.editProgram(false)
            state.updateStep(state.draft!!.steps.first().copy(f = 12.0))
            state.back()
        }
        rule.onNodeWithText("Save changes").performClick()
        rule.runOnIdle {
            assertEquals(Screen.PROGRAM, state.screen)
            assertNull(state.draft)
            assertEquals(12.0, state.customs.single().steps.first().f, 0.0)
            assertEquals(12.0, state.selected!!.steps.first().f, 0.0)
        }
    }

    @Test
    fun draftsRestoreLocallyAndDoNotLeakToAnotherProfile() {
        rule.runOnIdle {
            state.newProgram()
            state.draft = state.draft!!.copy(name = "Recover my draft")
            state.updateStep(state.draft!!.steps.first().copy(f = 13.0))
            state.save()
            val restored = AppState(rule.activity.application)
            assertEquals(Screen.BUILDER, restored.screen)
            assertTrue(restored.draftDirty)
            assertEquals("Recover my draft", restored.draft!!.name)
            assertEquals(13.0, restored.draft!!.steps.first().f, 0.0)
            assertTrue(restored.customs.isEmpty())
            val other = state.profiles.first { it.id != state.currentProfile }
            state.switchProfile(other.id)
            assertTrue(state.draftExitOpen)
            state.discardDraftAndLeave()
            assertEquals(other.id, state.currentProfile)
            val otherState = AppState(rule.activity.application)
            assertNull(otherState.draft)
            assertFalse(otherState.draftDirty)
        }
    }

    @Test
    fun historyReopensSavedResultsWithoutChangingHistoryOrBattery() {
        var record: SessionRecord? = null
        var battery = 0
        rule.runOnIdle {
            state.start(
                listOf(Program("ux-ended", "Ended session", steps = listOf(Step(dur = 10))))
            )
            state.engine!!.advance(6.0)
            state.finish(false)
            record = state.history.single()
            battery = state.battery
            assertEquals(10, record!!.plannedSeconds)
            assertEquals(state.user!!.name, record!!.profileName)
            assertEquals(20, record!!.simulationSpeed)
            assertEquals(record, recordFrom(record!!.json()))
            val legacy =
                record!!.json().apply {
                    remove("plannedSec")
                    remove("profileName")
                    remove("simulationSpeed")
                }
            assertEquals(0, recordFrom(legacy).plannedSeconds)
            state.openHistoryRecord(record!!)
        }
        rule.onNodeWithText("Session details").assertIsDisplayed()
        rule.onNodeWithText("Session Ended").assertIsDisplayed()
        rule.onNodeWithText("Stopped Early").assertIsDisplayed()
        rule.onNodeWithText("0:06 of 0:10").assertExists()
        snapshot("04-ended-details")
        rule.runOnIdle {
            assertEquals(1, state.history.size)
            assertEquals(battery, state.battery)
            assertTrue(state.summaryFromHistory)
            rule.activity.onBackPressedDispatcher.onBackPressed()
            assertEquals(Screen.HISTORY, state.screen)
        }
        rule.onNodeWithText("Ended session", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Session details").assertIsDisplayed()
        rule.runOnIdle { assertEquals(1, state.history.size) }
    }

    @Test
    fun quickStartCanBeNamedAndHomeActionsDoNotOverlapTheList() {
        rule.onNodeWithText("Simulation · 20×").assertIsDisplayed()
        rule.onNodeWithText("Choose a program").assertIsDisplayed()
        snapshot("01-home")
        rule.onNodeWithText("Quick Start").assertIsDisplayed().performClick()
        rule.onNodeWithText("Duration").assertIsDisplayed()
        rule.onNodeWithContentDescription("Fp1").assertDoesNotExist()
        snapshot("02-quick-start")
        rule.onNodeWithText("Save").performClick()
        snapshot("05-save-program", dialog = true)
        rule.onNode(hasSetTextAction()).performTextReplacement("My evening program")
        rule.onNode(hasText("Save program") and hasClickAction()).performClick()
        rule.waitUntil { state.screen == Screen.HOME }
        rule.runOnIdle { assertEquals("My evening program", state.customs.single().name) }
        rule.onNodeWithText("My evening program").performScrollTo().assertIsDisplayed()
        val quick = rule.onNodeWithText("Quick Start").fetchSemanticsNode().boundsInRoot
        val program = rule.onNodeWithText("My evening program").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "The reserved Quick Start area does not cover the program",
            program.bottom <= quick.top,
        )
    }

    @Test
    fun backNeverResumesAPausedSessionAndEarlyEndIsExplained() {
        rule.runOnIdle {
            state.speed = 1
            state.start(
                listOf(Program("ux-back", "Pause recovery", steps = listOf(Step(dur = 60))))
            )
            state.pauseInBackground()
        }
        rule.runOnIdle {
            rule.activity.onBackPressedDispatcher.onBackPressed()
            assertTrue(state.engine!!.paused)
            rule.activity.onBackPressedDispatcher.onBackPressed()
            assertTrue(state.engine!!.paused)
        }
        rule.onNodeWithText("Resume").assertIsDisplayed()
        snapshot("06-paused-context")
        rule.onNodeWithText("End session").assertIsDisplayed().performClick()
        rule.onNodeWithText("Session Ended").assertIsDisplayed()
        rule.onNodeWithText("This short session was not saved").assertExists()
        rule.runOnIdle { assertTrue(state.history.isEmpty()) }
    }

    @Test
    fun emptyLoginFocusesTheEmailFieldAndExplainsTheError() {
        rule.runOnIdle { state.loggedIn = false }
        rule.onNodeWithText("เข้าสู่ระบบ", useUnmergedTree = true).performScrollTo().performClick()
        rule.onNodeWithContentDescription("อีเมล").assertIsFocused()
        rule.onNodeWithText("กรอกอีเมลให้ถูกต้อง").assertExists()
        rule.runOnIdle { assertFalse(state.busy) }
    }
}
