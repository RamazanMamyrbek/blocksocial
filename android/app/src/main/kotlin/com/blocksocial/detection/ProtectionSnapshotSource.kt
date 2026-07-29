package com.blocksocial.detection

import com.blocksocial.core.data.catalog.SupportedAppCatalog
import com.blocksocial.core.data.repository.BlockEventRepository
import com.blocksocial.core.data.repository.RestrictedAppRepository
import com.blocksocial.core.data.repository.RestrictionRuleRepository
import com.blocksocial.core.data.repository.TemporaryAccessGrantRepository
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.BlockEvent
import com.blocksocial.core.model.UserAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtectionSnapshotSource @Inject constructor(
    private val catalog: SupportedAppCatalog,
    private val restrictedApps: RestrictedAppRepository,
    private val rules: RestrictionRuleRepository,
    private val grants: TemporaryAccessGrantRepository,
    private val blockEvents: BlockEventRepository,
) {

    fun snapshots(zone: () -> ZoneId = ZoneId::systemDefault): Flow<ProtectionSnapshot> = combine(
        restrictedApps.observeSelected(),
        rules.observeRulesByApp(),
        grants.observeAll(),
        blockEvents.observeRecent(RECENT_EVENT_WINDOW),
    ) { selectedApps, rulesByApp, activeGrants, recentEvents ->
        val selected = selectedApps.map { it.ref }.toSet()
        val packageToApp = catalog.load()
            .filter { it.ref in selected }
            .flatMap { app -> app.packageNames.map { it to app.ref } }
            .toMap()

        ProtectionSnapshot(
            packageToApp = packageToApp,
            displayNames = selectedApps.associate { it.ref to it.displayName },
            rulesByApp = rulesByApp.filterKeys { it in selected },
            grantsByApp = activeGrants.associateBy { it.forApp },
            todayCounts = countToday(recentEvents, zone()),
        )
    }

    private fun countToday(events: List<BlockEvent>, zone: ZoneId): Map<AppRef, BlockCounts> {
        val startOfToday = Instant.now().atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
        return events
            .filter { it.occurredAt >= startOfToday }
            .groupBy { it.restrictedAppRef }
            .mapValues { (_, appEvents) ->
                BlockCounts(
                    opens = appEvents.size,
                    endedHere = appEvents.count { it.userAction == UserAction.STAYED_FOCUSED },
                )
            }
    }

    private companion object {
        const val RECENT_EVENT_WINDOW = 500
    }
}
