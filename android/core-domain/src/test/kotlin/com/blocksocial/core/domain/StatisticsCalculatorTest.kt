package com.blocksocial.core.domain

import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.BlockEvent
import com.blocksocial.core.model.Platform
import com.blocksocial.core.model.RuleMode
import com.blocksocial.core.model.UserAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StatisticsCalculatorTest {

    private val zone = ZoneId.of("Asia/Tokyo")
    private val app = AppRef("youtube")
    private var sequence = 0

    private fun event(date: String, action: UserAction, hour: Int = 12): BlockEvent = BlockEvent(
        id = "event-${sequence++}",
        restrictedAppRef = app,
        occurredAt = Instant.parse("${date}T%02d:00:00+09:00".format(hour)),
        zone = zone,
        primaryReason = RuleMode.SCHEDULE,
        allReasons = listOf(RuleMode.SCHEDULE),
        userAction = action,
        bypassDurationMinutes = if (action == UserAction.BYPASSED) 5 else null,
        platform = Platform.ANDROID,
    )

    private fun day(date: String) = LocalDate.parse(date)

    @Test
    fun aDayCountsWhenBypassesAreAtOrBelowTheAllowance() {
        val outcome = DayOutcome(day("2026-07-28"), 3, 1, 2, 0)

        assertEquals(true, outcome.countsTowards(StreakPolicy.Default))
        assertEquals(false, DayOutcome(day("2026-07-28"), 4, 1, 3, 0).countsTowards(StreakPolicy.Default))
    }

    @Test
    fun aSingleBypassDoesNotBreakTheStreak() {
        val events = listOf(
            event("2026-07-27", UserAction.STAYED_FOCUSED),
            event("2026-07-28", UserAction.BYPASSED),
            event("2026-07-29", UserAction.STAYED_FOCUSED),
        )

        assertEquals(3, StatisticsCalculator.currentStreak(events, day("2026-07-29"), zone))
    }

    @Test
    fun exceedingTheAllowanceEndsTheStreakOnThatDayOnly() {
        val events = listOf(
            event("2026-07-26", UserAction.STAYED_FOCUSED),
            event("2026-07-27", UserAction.BYPASSED, hour = 9),
            event("2026-07-27", UserAction.BYPASSED, hour = 10),
            event("2026-07-27", UserAction.BYPASSED, hour = 11),
            event("2026-07-28", UserAction.STAYED_FOCUSED),
            event("2026-07-29", UserAction.STAYED_FOCUSED),
        )

        assertEquals(2, StatisticsCalculator.currentStreak(events, day("2026-07-29"), zone))
    }

    @Test
    fun aDayWithNoInterventionsCountsButTheStreakNeverPredatesTheFirstRecord() {
        val events = listOf(event("2026-07-28", UserAction.STAYED_FOCUSED))

        assertEquals(2, StatisticsCalculator.currentStreak(events, day("2026-07-29"), zone))
    }

    @Test
    fun withNothingRecordedThereIsNoStreakToClaim() {
        assertEquals(0, StatisticsCalculator.currentStreak(emptyList(), day("2026-07-29"), zone))
    }

    @Test
    fun theAllowanceIsConfigurable() {
        val events = listOf(
            event("2026-07-29", UserAction.BYPASSED, hour = 9),
            event("2026-07-29", UserAction.BYPASSED, hour = 10),
        )

        assertEquals(1, StatisticsCalculator.currentStreak(events, day("2026-07-29"), zone, StreakPolicy(2)))
        assertEquals(0, StatisticsCalculator.currentStreak(events, day("2026-07-29"), zone, StreakPolicy(1)))
    }

    @Test
    fun anUnresolvedBlockIsCountedAsNeitherARefusalNorABypass() {
        val events = listOf(
            event("2026-07-29", UserAction.STAYED_FOCUSED),
            event("2026-07-29", UserAction.DISMISSED_BY_SYSTEM),
            event("2026-07-29", UserAction.UNKNOWN),
        )

        val outcome = StatisticsCalculator.dailyOutcomes(events, zone).single()

        assertEquals(3, outcome.interventions)
        assertEquals(1, outcome.stayedFocused)
        assertEquals(0, outcome.bypassed)
        assertEquals(2, outcome.unresolved)
        assertEquals(1.0, StatisticsCalculator.refusalRate(listOf(outcome))!!, 0.0001)
    }

    @Test
    fun anUnresolvedBlockCannotSilentlyEndAStreak() {
        val events = List(5) { event("2026-07-29", UserAction.DISMISSED_BY_SYSTEM) }

        assertEquals(1, StatisticsCalculator.currentStreak(events, day("2026-07-29"), zone))
    }

    @Test
    fun aRateWithNothingToDivideIsHiddenRatherThanShownAsZero() {
        val nothingDecided = listOf(DayOutcome(day("2026-07-29"), 2, 0, 0, 2))

        assertNull(StatisticsCalculator.refusalRate(nothingDecided))
        assertNull(StatisticsCalculator.refusalRate(emptyList()))
    }

    @Test
    fun daysAreGroupedInTheZoneTheUserLivedThrough() {
        val events = listOf(
            event("2026-07-28", UserAction.STAYED_FOCUSED, hour = 23),
            event("2026-07-29", UserAction.STAYED_FOCUSED, hour = 1),
        )

        val outcomes = StatisticsCalculator.dailyOutcomes(events, zone)

        assertEquals(listOf(day("2026-07-29"), day("2026-07-28")), outcomes.map { it.date })
    }
}
