package com.blocksocial.feature.rules

import com.blocksocial.core.model.RestrictionRule
import com.blocksocial.core.model.RuleMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class RuleDraftTest {

    private fun schedule(
        start: LocalTime = LocalTime.of(22, 0),
        end: LocalTime = LocalTime.of(7, 0),
        days: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY),
    ) = RuleDraft(
        id = "r1",
        mode = RuleMode.SCHEDULE,
        daysOfWeek = days,
        startLocalTime = start,
        endLocalTime = end,
    )

    @Test
    fun anOvernightIntervalIsRecognisedAndSplitAtMidnight() {
        val interval = schedule().interval

        assertTrue(interval.crossesMidnight)
        assertEquals(120, interval.firstSegmentMinutes)
        assertEquals(420, interval.secondSegmentMinutes)
        assertEquals(540, interval.totalMinutes)
    }

    @Test
    fun aSameDayIntervalIsNotSplit() {
        val interval = schedule(start = LocalTime.of(18, 0), end = LocalTime.of(21, 0)).interval

        assertFalse(interval.crossesMidnight)
        assertEquals(180, interval.firstSegmentMinutes)
        assertEquals(0, interval.secondSegmentMinutes)
    }

    @Test
    fun anIntervalCannotBeReadAsItsInverse() {
        val evening = schedule(start = LocalTime.of(22, 0), end = LocalTime.of(7, 0)).interval
        val morning = schedule(start = LocalTime.of(7, 0), end = LocalTime.of(22, 0)).interval

        assertTrue(evening.crossesMidnight)
        assertFalse(morning.crossesMidnight)
        assertEquals(540, evening.totalMinutes)
        assertEquals(900, morning.totalMinutes)
    }

    @Test
    fun aScheduleWithNoDaysIsRejected() {
        val draft = schedule(days = emptySet())

        assertEquals(listOf(RuleProblem.NO_DAYS_SELECTED), draft.problems())
        assertNull(draft.toRule())
    }

    @Test
    fun anIntervalOfZeroLengthIsRejected() {
        val draft = schedule(start = LocalTime.of(22, 0), end = LocalTime.of(22, 0))

        assertTrue(RuleProblem.INTERVAL_IS_EMPTY in draft.problems())
        assertNull(draft.toRule())
    }

    @Test
    fun anAlwaysOnRuleNeedsNothingElse() {
        val draft = RuleDraft(id = "r1", mode = RuleMode.ALWAYS)

        assertTrue(draft.isValid)
        assertTrue(draft.toRule() is RestrictionRule.AlwaysOn)
    }

    @Test
    fun aDailyLimitOutsideTheAllowedRangeIsRejected() {
        val tooSmall = RuleDraft(id = "r1", mode = RuleMode.DAILY_LIMIT, dailyLimitMinutes = 1)
        val tooLarge = RuleDraft(id = "r1", mode = RuleMode.DAILY_LIMIT, dailyLimitMinutes = 10_000)

        assertEquals(listOf(RuleProblem.LIMIT_OUT_OF_RANGE), tooSmall.problems())
        assertEquals(listOf(RuleProblem.LIMIT_OUT_OF_RANGE), tooLarge.problems())
    }

    @Test
    fun aStoredRuleRoundTripsThroughTheDraft() {
        val original = RestrictionRule.Schedule(
            id = "r9",
            enabled = false,
            daysOfWeek = setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
            startLocalTime = LocalTime.of(23, 30),
            endLocalTime = LocalTime.of(6, 15),
        )

        assertEquals(original, RuleDraft.of(original).toRule())
    }

    @Test
    fun pausingARuleKeepsEverythingElse() {
        val original = RestrictionRule.Schedule(
            id = "r9",
            enabled = true,
            daysOfWeek = setOf(DayOfWeek.FRIDAY),
            startLocalTime = LocalTime.of(23, 30),
            endLocalTime = LocalTime.of(6, 15),
        )

        val paused = RuleDraft.of(original).copy(enabled = false).toRule()

        assertEquals(original.copy(enabled = false), paused)
    }
}
