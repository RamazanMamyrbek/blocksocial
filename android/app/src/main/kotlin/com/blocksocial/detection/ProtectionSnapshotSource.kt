package com.blocksocial.detection

import com.blocksocial.catalog.SupportedAppCatalog
import com.blocksocial.core.data.repository.RestrictedAppRepository
import com.blocksocial.core.data.repository.RestrictionRuleRepository
import com.blocksocial.core.data.repository.TemporaryAccessGrantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtectionSnapshotSource @Inject constructor(
    private val catalog: SupportedAppCatalog,
    private val restrictedApps: RestrictedAppRepository,
    private val rules: RestrictionRuleRepository,
    private val grants: TemporaryAccessGrantRepository,
) {

    fun snapshots(): Flow<ProtectionSnapshot> = combine(
        restrictedApps.observeSelected(),
        rules.observeRulesByApp(),
        grants.observeAll(),
    ) { selectedApps, rulesByApp, activeGrants ->
        val selected = selectedApps.map { it.ref }.toSet()
        val packageToApp = catalog.load()
            .filter { it.ref in selected }
            .flatMap { app -> app.packageNames.map { it to app.ref } }
            .toMap()

        ProtectionSnapshot(
            packageToApp = packageToApp,
            rulesByApp = rulesByApp.filterKeys { it in selected },
            grantsByApp = activeGrants.associateBy { it.forApp },
        )
    }
}
