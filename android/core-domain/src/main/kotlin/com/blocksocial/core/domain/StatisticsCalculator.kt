package com.blocksocial.core.domain

import com.blocksocial.core.model.BlockEvent
import com.blocksocial.core.model.UserAction
import java.time.LocalDate
import java.time.ZoneId

data class StreakPolicy(val maximumBypassesPerDay: Int) {
    companion object {
        val Default = StreakPolicy(maximumBypassesPerDay = 2)
    }
}

data class DayOutcome(
    val date: LocalDate,
    val interventions: Int,
    val stayedFocused: Int,
    val bypassed: Int,
    val unresolved: Int,
) {
    val decided: Int = stayedFocused + bypassed

    fun countsTowards(policy: StreakPolicy): Boolean = bypassed <= policy.maximumBypassesPerDay
}

object StatisticsCalculator {

    fun dailyOutcomes(events: List<BlockEvent>, zone: ZoneId): List<DayOutcome> = events
        .groupBy { it.occurredAt.atZone(zone).toLocalDate() }
        .map { (date, ofDay) ->
            DayOutcome(
                date = date,
                interventions = ofDay.size,
                stayedFocused = ofDay.count { it.userAction == UserAction.STAYED_FOCUSED },
                bypassed = ofDay.count { it.userAction == UserAction.BYPASSED },
                unresolved = ofDay.count { it.userAction.isUnresolved() },
            )
        }
        .sortedByDescending { it.date }

    fun currentStreak(
        events: List<BlockEvent>,
        today: LocalDate,
        zone: ZoneId,
        policy: StreakPolicy = StreakPolicy.Default,
    ): Int {
        val outcomes = dailyOutcomes(events, zone).associateBy { it.date }
        val firstWatchedDay = outcomes.keys.minOrNull() ?: return 0

        var day = today
        var streak = 0
        while (!day.isBefore(firstWatchedDay)) {
            val qualifies = outcomes[day]?.countsTowards(policy) ?: true
            if (!qualifies) break
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    fun refusalRate(outcomes: List<DayOutcome>): Double? {
        val decided = outcomes.sumOf { it.stayedFocused + it.bypassed }
        if (decided == 0) return null
        return outcomes.sumOf { it.stayedFocused }.toDouble() / decided
    }

    private fun UserAction.isUnresolved(): Boolean = when (this) {
        UserAction.DISMISSED_BY_SYSTEM, UserAction.UNKNOWN -> true
        UserAction.STAYED_FOCUSED, UserAction.BYPASSED -> false
    }
}
