package com.blocksocial.home

import com.blocksocial.core.data.repository.BlockEventRepository
import com.blocksocial.core.data.repository.RestrictionRuleRepository
import com.blocksocial.core.domain.ProtectionBanner
import com.blocksocial.core.domain.StatisticsCalculator
import com.blocksocial.feature.appselection.AppSelectionController
import com.blocksocial.feature.appselection.InstallState
import com.blocksocial.feature.rules.RuleDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeController @Inject constructor(
    private val apps: AppSelectionController,
    private val rules: RestrictionRuleRepository,
    private val blockEvents: BlockEventRepository,
) {

    fun state(
        protection: Flow<ProtectionBanner>,
        zone: () -> ZoneId = ZoneId::systemDefault,
        today: () -> LocalDate = LocalDate::now,
    ): Flow<HomeState> = combine(
        apps.rows(),
        rules.observeRulesByApp(),
        blockEvents.observeRecent(RECENT_EVENT_WINDOW),
        protection,
    ) { rows, rulesByApp, events, banner ->
        val outcomes = StatisticsCalculator.dailyOutcomes(events, zone())
        val todayOutcome = outcomes.firstOrNull { it.date == today() }

        HomeState(
            protection = banner,
            apps = rows
                .filter { it.selected }
                .map { row ->
                    RestrictedAppRow(
                        ref = row.ref,
                        displayName = row.displayName,
                        rules = rulesByApp[row.ref].orEmpty(),
                        installed = row.installState == InstallState.INSTALLED,
                    )
                },
            stayedFocusedToday = todayOutcome?.stayedFocused ?: 0,
            decisionsToday = todayOutcome?.decided ?: 0,
        )
    }

    suspend fun setPaused(row: RestrictedAppRow, paused: Boolean) {
        val now = Instant.now()
        row.rules.forEach { rule ->
            val updated = RuleDraft.of(rule).copy(enabled = !paused).toRule() ?: return@forEach
            rules.save(updated, row.ref, now, now)
        }
    }

    private companion object {
        const val RECENT_EVENT_WINDOW = 500
    }
}
