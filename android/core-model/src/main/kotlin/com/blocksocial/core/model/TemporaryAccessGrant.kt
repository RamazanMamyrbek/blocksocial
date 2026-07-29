package com.blocksocial.core.model

import java.time.Instant

data class TemporaryAccessGrant(
    val forApp: AppRef,
    val grantedAtWallClock: Instant,
    val grantedAtMonotonicMillis: Long,
    val durationMinutes: Int,
)
