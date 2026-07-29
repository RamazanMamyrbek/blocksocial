package com.blocksocial.core.model

import java.time.Instant

data class UsageSession(
    val forApp: AppRef,
    val from: Instant,
    val to: Instant?,
)
