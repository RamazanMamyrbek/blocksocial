package com.blocksocial.spike.a04

data class DeviceTime(
    val wallClockMillis: Long,
    val elapsedRealtimeMillis: Long
)

data class TemporaryAccessGrant(
    val packageName: String,
    val grantedAtWallClockMillis: Long,
    val expiresAtWallClockMillis: Long,
    val grantedAtElapsedRealtimeMillis: Long,
    val expiresAtElapsedRealtimeMillis: Long
)

enum class GrantEvaluation {
    ACTIVE,
    EXPIRED,
    ACTIVE_AFTER_REBOOT,
    EXPIRED_AFTER_REBOOT,
    DISCARDED_CLOCK_MOVED_BEFORE_GRANT
}

val GrantEvaluation.suppressesBlock: Boolean
    get() = this == GrantEvaluation.ACTIVE || this == GrantEvaluation.ACTIVE_AFTER_REBOOT

object GrantEvaluator {

    fun newGrant(
        packageName: String,
        durationMillis: Long,
        now: DeviceTime
    ) = TemporaryAccessGrant(
        packageName = packageName,
        grantedAtWallClockMillis = now.wallClockMillis,
        expiresAtWallClockMillis = now.wallClockMillis + durationMillis,
        grantedAtElapsedRealtimeMillis = now.elapsedRealtimeMillis,
        expiresAtElapsedRealtimeMillis = now.elapsedRealtimeMillis + durationMillis
    )

    fun evaluate(grant: TemporaryAccessGrant, now: DeviceTime): GrantEvaluation {
        val rebooted = now.elapsedRealtimeMillis < grant.grantedAtElapsedRealtimeMillis
        if (!rebooted) {
            return if (now.elapsedRealtimeMillis < grant.expiresAtElapsedRealtimeMillis) {
                GrantEvaluation.ACTIVE
            } else {
                GrantEvaluation.EXPIRED
            }
        }
        if (now.wallClockMillis < grant.grantedAtWallClockMillis) {
            return GrantEvaluation.DISCARDED_CLOCK_MOVED_BEFORE_GRANT
        }
        return if (now.wallClockMillis < grant.expiresAtWallClockMillis) {
            GrantEvaluation.ACTIVE_AFTER_REBOOT
        } else {
            GrantEvaluation.EXPIRED_AFTER_REBOOT
        }
    }
}
