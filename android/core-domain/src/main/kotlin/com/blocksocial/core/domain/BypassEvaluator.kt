package com.blocksocial.core.domain

import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.DeviceTime
import com.blocksocial.core.model.TemporaryAccessGrant
import java.time.temporal.ChronoUnit

enum class GrantEvaluation {
    ACTIVE,
    EXPIRED,
    ACTIVE_AFTER_REBOOT,
    EXPIRED_AFTER_REBOOT,
    DISCARDED_CLOCK_MOVED_BEFORE_GRANT,
}

data class BypassDecision(
    val suppressesBlock: Boolean,
    val evaluation: GrantEvaluation?,
)

object BypassEvaluator {

    private val NoApplicableGrant = BypassDecision(suppressesBlock = false, evaluation = null)

    fun evaluate(grant: TemporaryAccessGrant?, forApp: AppRef, at: DeviceTime): BypassDecision {
        if (grant == null || grant.forApp != forApp) return NoApplicableGrant

        val rebooted = at.monotonicMillis < grant.grantedAtMonotonicMillis
        if (!rebooted) {
            val elapsedMillis = at.monotonicMillis - grant.grantedAtMonotonicMillis
            return if (elapsedMillis < grant.durationMinutes * MILLIS_PER_MINUTE) {
                BypassDecision(true, GrantEvaluation.ACTIVE)
            } else {
                BypassDecision(false, GrantEvaluation.EXPIRED)
            }
        }

        if (at.wallClock < grant.grantedAtWallClock) {
            return BypassDecision(false, GrantEvaluation.DISCARDED_CLOCK_MOVED_BEFORE_GRANT)
        }

        val expiresAt =
            grant.grantedAtWallClock.plus(grant.durationMinutes.toLong(), ChronoUnit.MINUTES)
        return if (at.wallClock < expiresAt) {
            BypassDecision(true, GrantEvaluation.ACTIVE_AFTER_REBOOT)
        } else {
            BypassDecision(false, GrantEvaluation.EXPIRED_AFTER_REBOOT)
        }
    }

    private const val MILLIS_PER_MINUTE = 60_000L
}
