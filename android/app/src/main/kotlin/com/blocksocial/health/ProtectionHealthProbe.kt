package com.blocksocial.health

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.blocksocial.core.data.preferences.PermissionSnapshot
import com.blocksocial.core.data.preferences.PreferencesRepository
import com.blocksocial.core.domain.AccessibilityObservation
import com.blocksocial.core.domain.ProtectionHealthReducer
import com.blocksocial.core.domain.ProtectionRequirement
import com.blocksocial.core.domain.RequirementHealth
import com.blocksocial.detection.BlockSocialAccessibilityService
import com.blocksocial.usage.UsageStatsReader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class ProtectionHealth(
    val items: List<RequirementHealth>,
    val blockingRuns: Boolean,
) {
    companion object {
        val Unknown = ProtectionHealth(emptyList(), blockingRuns = false)
    }
}

@Singleton
class ProtectionHealthProbe @Inject constructor(
    @ApplicationContext private val context: Context,
    private val heartbeat: ServiceHeartbeat,
    private val usageStats: UsageStatsReader,
    private val preferences: PreferencesRepository,
) {

    fun startProbe(now: Instant = Instant.now()) = heartbeat.startProbe(now)

    fun accessibilityEnabledInSettings(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        val component = ComponentName(context, BlockSocialAccessibilityService::class.java)
        return enabled.split(':').any {
            it.equals(component.flattenToString(), ignoreCase = true) ||
                it.equals(component.flattenToShortString(), ignoreCase = true)
        }
    }

    suspend fun read(now: Instant = Instant.now()): ProtectionHealth {
        val snapshot = preferences.preferences.first().permissionSnapshot
        val health = assemble(now, snapshot)
        preferences.setPermissionSnapshot(
            PermissionSnapshot(
                accessibilityServiceEnabled = accessibilityEnabledInSettings(),
                usageAccessGranted = usageStats.hasUsageAccess(),
                notificationsGranted = NotificationManagerCompat.from(context).areNotificationsEnabled(),
                capturedAt = now,
            ),
        )
        return health
    }

    fun observe(now: () -> Instant = Instant::now): Flow<ProtectionHealth> =
        preferences.preferences.map { assemble(now(), it.permissionSnapshot) }

    private fun assemble(now: Instant, snapshot: PermissionSnapshot): ProtectionHealth {
        val everAsked = snapshot.capturedAt != null

        val items = listOf(
            RequirementHealth(
                requirement = ProtectionRequirement.ACCESSIBILITY_SERVICE,
                status = ProtectionHealthReducer.accessibility(
                    AccessibilityObservation(
                        enabledInSettings = accessibilityEnabledInSettings(),
                        everAsked = everAsked,
                        connectedInThisProcess = heartbeat.isConnected,
                        lastEventAt = heartbeat.lastEvent,
                        probeStartedAt = heartbeat.probeStarted,
                        now = now,
                    ),
                ),
            ),
            RequirementHealth(
                requirement = ProtectionRequirement.USAGE_ACCESS,
                status = ProtectionHealthReducer.granted(usageStats.hasUsageAccess(), everAsked),
            ),
            RequirementHealth(
                requirement = ProtectionRequirement.NOTIFICATIONS,
                status = ProtectionHealthReducer.granted(
                    NotificationManagerCompat.from(context).areNotificationsEnabled(),
                    everAsked,
                ),
            ),
        )

        return ProtectionHealth(items = items, blockingRuns = ProtectionHealthReducer.blockingRuns(items))
    }
}
