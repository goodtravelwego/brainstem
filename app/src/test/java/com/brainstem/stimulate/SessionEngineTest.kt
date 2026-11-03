package com.brainstem.stimulate

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class SessionEngineTest {
    @Test
    fun weekStripRoundsEachDayAndExcludesOutsideTheSevenDayWindow() {
        val today = java.time.LocalDate.of(2026, 11, 2)
        fun record(daysAgo: Long, seconds: Int) =
            SessionRecord(
                date =
                    today
                        .minusDays(daysAgo)
                        .atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                complete = false,
                seconds = seconds,
                level = 1,
                items = emptyList(),
                dose = emptyMap(),
            )
        val minutes =
            weeklyMinutes(
                listOf(
                    record(0, 29),
                    record(1, 29),
                    record(2, 31),
                    record(7, 600),
                    record(-1, 600),
                ),
                today,
            )
        assertEquals(listOf(0, 0, 0, 0, 1, 0, 0), minutes)
        assertEquals(1, minutes.sum())
    }

    @Test
    fun seededClosedLoopReplaySurvivesDifferentFrameSizes() {
        val plan =
            makePlan(
                listOf(Program("test", "Test", steps = listOf(Step(dur = 120)))),
                3,
                true,
                false,
            )
        val first = SessionEngine(plan, Spec(), 42L)
        repeat(1200) { first.advance(.2) }
        val restored = SessionEngine(plan, Spec(), 42L)
        restored.advance(first.elapsed)
        assertEquals(first.iapf, restored.iapf, 0.0)
        assertEquals(first.trace, restored.trace)
        assertEquals(first.events, restored.events)
        assertEquals(first.level, restored.level)
        assertArrayEquals(first.eeg, restored.eeg, 0.0)
        assertEquals(first.record(true).deltas, restored.record(true).deltas)
    }

    @Test
    fun eegIsSampledOnlyWithLightOffAndResponseDependsOnFrequency() {
        fun engine(f: Double) =
            SessionEngine(
                makePlan(
                    listOf(Program("test", "Test", steps = listOf(Step(f = f, dur = 120)))),
                    1,
                    true,
                    false,
                ),
                Spec(),
                24L,
            )
        val alpha = engine(10.0)
        alpha.advance(60.0)
        assertNull(alpha.bands)
        assertTrue(alpha.trace.isEmpty())
        alpha.advance(60.0)
        assertNull(alpha.bands)
        alpha.advance(120.0)
        val gamma = engine(40.0).apply { advance(240.0) }
        assertEquals(1, alpha.trace.size)
        assertTrue(gamma.record(true).deltas[1] > alpha.record(true).deltas[1])
        assertTrue(gamma.record(true).deltas[2] < alpha.record(true).deltas[2])
        assertEquals(.24, alpha.dose.getValue("F3"), 1e-8)
    }

    @Test
    fun headsetRequiresCleanContactsAndMainsThenCachesSameDayFlow() {
        val model = HeadsetCheckModel(false, Random(4))
        assertFalse(model.ready)
        repeat(100) {
            model.advance(.1)
            if (model.fixChannel != null || model.needsHumFix) model.fix()
        }
        assertTrue(model.ready)
        assertFalse(model.automatic)
        val fresh = HeadsetCheckModel(true, Random(5))
        repeat(100) {
            fresh.advance(.1)
            if (fresh.fixChannel != null || fresh.needsHumFix) fresh.fix()
        }
        assertTrue(fresh.ready)
        assertTrue(fresh.automatic)
    }

    @Test
    fun fixedDoseUsesLevelPowerDutyAndSelectedPoints() {
        val p =
            Program(
                "test",
                "Test",
                steps = listOf(Step(pts = listOf("F3"), dur = 60, power = 50, duty = 40)),
            )
        val r = SessionEngine(makePlan(listOf(p), 2, false, false), Spec(irradiance = 40))
        r.advance(60.0)
        assertTrue(r.finished)
        assertEquals(.096, r.dose.getValue("F3"), 1e-8)
        assertEquals(0.0, r.dose.getValue("F4"), 0.0)
        assertEquals(60.0, r.elapsed, 0.0)
    }

    @Test
    fun advanceSplitsAtStepBoundariesAndNeverExceedsPlan() {
        val p =
            Program(
                "test",
                "Test",
                steps =
                    listOf(Step(pts = listOf("F3"), dur = 10), Step(pts = listOf("F4"), dur = 20)),
            )
        val r = SessionEngine(makePlan(listOf(p), 1, false, true), Spec())
        r.advance(100.0)
        assertTrue(r.finished)
        assertEquals(30.0, r.elapsed, 0.0)
        assertEquals(.02, r.dose.getValue("F3"), 1e-8)
        assertEquals(.04, r.dose.getValue("F4"), 1e-8)
        assertEquals(30, r.record(true).items.single().seconds)
    }

    @Test
    fun pauseFreezesDoseAndClock() {
        val r = SessionEngine(makePlan(listOf(Program("test", "Test")), 1, false, false), Spec())
        r.advance(5.0)
        r.paused = true
        r.advance(30.0)
        assertEquals(5.0, r.elapsed, 0.0)
        assertEquals(.01, r.dose.getValue("F3"), 1e-8)
    }

    @Test
    fun closedLoopInsertsBaselineAndMeasurementWithoutLight() {
        val plan =
            makePlan(
                listOf(Program("test", "Test", steps = listOf(Step(dur = 600)))),
                1,
                true,
                false,
            )
        assertEquals(listOf(60, 150, 45, 150, 45, 150, 45, 150, 60), plan.steps.map { it.dur })
        val r = SessionEngine(plan, Spec())
        r.advance(60.0)
        assertEquals(0.0, r.dose.values.sum(), 0.0)
        r.advance(1000.0)
        assertEquals(855.0, r.elapsed, 0.0)
        assertEquals(1.2, r.dose.getValue("F3"), 1e-8)
    }

    @Test
    fun sweepIncludesBothEndpointsAndCanDescend() {
        val s = Step(mode = "sweep", f1 = 8.0, f2 = 12.0, dur = 50)
        assertEquals(8.0, frequencyAt(s, 0.0), 0.0)
        assertEquals(9.0, frequencyAt(s, 10.0), 0.0)
        assertEquals(12.0, frequencyAt(s, 49.9), 0.0)
        assertEquals(8.0, frequencyAt(s.copy(f1 = 12.0, f2 = 8.0), 49.9), 0.0)
    }

    @Test
    fun scalingPreservesRelativeStepLengths() {
        val p = Program("test", "Test", steps = listOf(Step(dur = 120), Step(dur = 240)))
        assertEquals(listOf(60, 120), p.scaled(3).steps.map { it.dur })
    }
}
