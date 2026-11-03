package com.brainstem.stimulate

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModelProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Uses only the isolated verification application's local guest data. */
class SessionRestorationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val state
        get() = ViewModelProvider(rule.activity)[AppState::class.java]

    @After
    fun clearPendingSession() {
        rule.runOnIdle {
            state.engine = null
            state.queue.clear()
            state.save()
        }
    }

    @Test
    fun activityRecreationKeepsSessionAndPausesIt() {
        lateinit var original: AppState
        lateinit var running: SessionEngine
        rule.runOnIdle {
            state.guest()
            state.speed = 1
            state.closedLoop = false
            state.start(listOf(Program("rotation-test", "Rotation test", steps = listOf(Step()))))
            state.engine!!.advance(18.7)
            original = state
            running = state.engine!!
        }

        rule.activityRule.scenario.recreate()

        rule.runOnIdle {
            assertSame("Rotation retains the ViewModel", original, state)
            assertSame("Rotation retains the running session", running, state.engine)
            assertTrue("Leaving the activity pauses the session", running.paused)
            assertTrue("Progress is retained", running.elapsed >= 18.7)
        }
        rule.onNodeWithText("Resume").assertIsDisplayed()
        val stoppedAt = running.elapsed
        Thread.sleep(300)
        rule.runOnIdle { assertEquals(stoppedAt, running.elapsed, 0.0) }
    }

    @Test
    fun savedSessionRestoresProfileQueueDoseAndClosedLoopState() {
        rule.runOnIdle {
            state.guest()
            state.engine = null
            state.addLocalProfile("Restoration test", 30)
            state.pro = true
            state.level = 4
            state.closedLoop = true
            state.speed = 1
            state.spec = Spec(850, 32, 40, 30)
            val program =
                Program(
                    "restoration-test",
                    "Restoration test",
                    steps = listOf(Step(pts = listOf("F3", "F4"), dur = 300)),
                )
            state.customs.add(program)
            state.favorites.add(program.id)
            state.queue.clear()
            state.queue.add(QueueItem(program.id, 5))
            state.start(listOf(program), fromQueue = true)
            val original = state.engine!!
            original.advance(94.7)
            state.pauseInBackground()

            // A fresh ViewModel reads Android preferences rather than retained activity state.
            val restoredState = AppState(rule.activity.application)
            val restored = requireNotNull(restoredState.engine)
            assertTrue(restoredState.loggedIn)
            assertEquals(state.currentProfile, restoredState.currentProfile)
            assertEquals("Restoration test", restoredState.user!!.name)
            assertEquals(state.queue.toList(), restoredState.queue.toList())
            assertEquals(state.customs.toList(), restoredState.customs.toList())
            assertEquals(state.favorites.toList(), restoredState.favorites.toList())
            assertEquals(state.spec, restoredState.spec)
            assertTrue("Restoration requires an explicit resume", restored.paused)
            assertEquals(original.seed, restored.seed)
            assertEquals(original.plan, restored.plan)
            assertEquals(original.elapsed, restored.elapsed, 0.0)
            assertEquals(original.index, restored.index)
            assertEquals(original.stepElapsed, restored.stepElapsed, 0.0)
            assertEquals(original.dose, restored.dose)
            assertArrayEquals(original.eeg, restored.eeg, 0.0)
            assertEquals(original.events, restored.events)

            // Resuming must continue from the saved dose and seeded EEG, without duplication.
            original.paused = false
            restored.paused = false
            original.advance(35.0)
            restored.advance(35.0)
            assertEquals(original.elapsed, restored.elapsed, 0.0)
            assertEquals(original.dose, restored.dose)
            assertArrayEquals(original.eeg, restored.eeg, 0.0)
            assertEquals(original.events, restored.events)
            state.pauseInBackground()
        }
    }
}
