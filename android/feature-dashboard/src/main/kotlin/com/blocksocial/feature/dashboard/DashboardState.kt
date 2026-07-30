package com.blocksocial.feature.dashboard

import com.blocksocial.core.domain.ProtectionBanner

data class DashboardState(
    val protection: ProtectionBanner,
    val interventionsToday: Int,
    val stayedFocusedToday: Int,
    val bypassedToday: Int,
    val refusalRateLastSevenDays: Double?,
    val activeRules: Int,
    val pausedRules: Int,
    val streakDays: Int,
    val bypassAllowancePerDay: Int,
) {
    companion object {
        val Empty = DashboardState(
            protection = ProtectionBanner.NEVER_SET_UP,
            interventionsToday = 0,
            stayedFocusedToday = 0,
            bypassedToday = 0,
            refusalRateLastSevenDays = null,
            activeRules = 0,
            pausedRules = 0,
            streakDays = 0,
            bypassAllowancePerDay = 2,
        )
    }
}
