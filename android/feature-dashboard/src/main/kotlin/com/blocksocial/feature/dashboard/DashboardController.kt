package com.blocksocial.feature.dashboard

import com.blocksocial.core.data.repository.BlockEventRepository
import com.blocksocial.core.data.repository.RestrictionRuleRepository
import com.blocksocial.core.domain.StatisticsCalculator
import com.blocksocial.core.domain.StreakPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardController @Inject constructor(
    private val blockEvents: BlockEventRepository,
    private val rules: RestrictionRuleRepository,
) {

    fun state(
        policy: StreakPolicy = StreakPolicy.Default,
        zone: () -> ZoneId = ZoneId::systemDefault,
        today: () -> LocalDate = LocalDate::now,
    ): Flow<DashboardState> = combine(
        blockEvents.observeRecent(RECENT_EVENT_WINDOW),
        rules.observeRulesByApp(),
    ) { events, rulesByApp ->
        val currentZone = zone()
        val currentDay = today()
        val outcomes = StatisticsCalculator.dailyOutcomes(events, currentZone)
        val todayOutcome = outcomes.firstOrNull { it.date == currentDay }
        val lastSevenDays = outcomes.filter { it.date > currentDay.minusDays(7) }
        val allRules = rulesByApp.values.flatten()

        DashboardState(
            interventionsToday = todayOutcome?.interventions ?: 0,
            stayedFocusedToday = todayOutcome?.stayedFocused ?: 0,
            bypassedToday = todayOutcome?.bypassed ?: 0,
            refusalRateLastSevenDays = StatisticsCalculator.refusalRate(lastSevenDays),
            activeRules = allRules.count { it.enabled },
            pausedRules = allRules.count { !it.enabled },
            streakDays = StatisticsCalculator.currentStreak(events, currentDay, currentZone, policy),
            bypassAllowancePerDay = policy.maximumBypassesPerDay,
        )
    }

    private companion object {
        const val RECENT_EVENT_WINDOW = 500
    }
}
