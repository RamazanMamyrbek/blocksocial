package com.blocksocial.core.domain

import java.time.Duration
import java.time.Instant

enum class ProtectionRequirement { ACCESSIBILITY_SERVICE, USAGE_ACCESS, NOTIFICATIONS }

enum class RequirementStatus {
    HEALTHY,
    NOT_ASKED,
    DENIED,
    ENABLED_BUT_NOT_RUNNING,
    RUNNING_BUT_SILENT,
}

data class AccessibilityObservation(
    val enabledInSettings: Boolean,
    val everAsked: Boolean,
    val connectedInThisProcess: Boolean,
    val lastEventAt: Instant?,
    val probeStartedAt: Instant?,
    val now: Instant,
)

data class RequirementHealth(
    val requirement: ProtectionRequirement,
    val status: RequirementStatus,
) {
    val blocksProtection: Boolean = when (status) {
        RequirementStatus.HEALTHY -> false
        RequirementStatus.NOT_ASKED,
        RequirementStatus.DENIED,
        RequirementStatus.ENABLED_BUT_NOT_RUNNING,
        RequirementStatus.RUNNING_BUT_SILENT,
        -> requirement == ProtectionRequirement.ACCESSIBILITY_SERVICE
    }
}

object ProtectionHealthReducer {

    val PROBE_GRACE: Duration = Duration.ofSeconds(3)

    fun accessibility(observation: AccessibilityObservation): RequirementStatus {
        if (!observation.enabledInSettings) {
            return if (observation.everAsked) RequirementStatus.DENIED else RequirementStatus.NOT_ASKED
        }
        if (!observation.connectedInThisProcess) return RequirementStatus.ENABLED_BUT_NOT_RUNNING

        val probeStartedAt = observation.probeStartedAt ?: return RequirementStatus.HEALTHY
        if (observation.now < probeStartedAt.plus(PROBE_GRACE)) return RequirementStatus.HEALTHY

        val lastEventAt = observation.lastEventAt
        val answeredTheProbe = lastEventAt != null && !lastEventAt.isBefore(probeStartedAt)
        return if (answeredTheProbe) RequirementStatus.HEALTHY else RequirementStatus.RUNNING_BUT_SILENT
    }

    fun granted(granted: Boolean, everAsked: Boolean): RequirementStatus = when {
        granted -> RequirementStatus.HEALTHY
        everAsked -> RequirementStatus.DENIED
        else -> RequirementStatus.NOT_ASKED
    }

    fun blockingRuns(items: List<RequirementHealth>): Boolean = items.none { it.blocksProtection }
}
