package com.blocksocial.home

import com.blocksocial.core.domain.ProtectionBanner
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.RestrictionRule

data class RestrictedAppRow(
    val ref: AppRef,
    val displayName: String,
    val rules: List<RestrictionRule>,
    val installed: Boolean,
) {
    val liveRule: RestrictionRule? = rules.firstOrNull { it.enabled }
    val paused: Boolean = liveRule == null
}

data class HomeState(
    val protection: ProtectionBanner,
    val apps: List<RestrictedAppRow>,
    val stayedFocusedToday: Int,
    val interventionsToday: Int,
) {
    val nothingRestrictedYet: Boolean = apps.isEmpty()

    companion object {
        val Empty = HomeState(
            protection = ProtectionBanner.NEVER_SET_UP,
            apps = emptyList(),
            stayedFocusedToday = 0,
            interventionsToday = 0,
        )
    }
}
