package com.blocksocial.core.model

data class BypassPolicy(val grantDurationMinutes: Int) {

    fun grant(forApp: AppRef, at: DeviceTime): TemporaryAccessGrant = TemporaryAccessGrant(
        forApp = forApp,
        grantedAtWallClock = at.wallClock,
        grantedAtMonotonicMillis = at.monotonicMillis,
        durationMinutes = grantDurationMinutes,
    )

    companion object {
        val Android = BypassPolicy(grantDurationMinutes = 5)
    }
}
