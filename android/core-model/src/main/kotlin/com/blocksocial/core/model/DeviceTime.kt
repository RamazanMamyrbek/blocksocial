package com.blocksocial.core.model

import java.time.Instant
import java.time.ZoneId

data class DeviceTime(
    val wallClock: Instant,
    val monotonicMillis: Long,
    val zone: ZoneId,
)
