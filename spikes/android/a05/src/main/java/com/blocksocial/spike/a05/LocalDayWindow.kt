package com.blocksocial.spike.a05

import java.time.Instant
import java.time.ZoneId

data class LocalDayWindow(
    val startMillis: Long,
    val endMillis: Long
) {
    val lengthMillis: Long get() = endMillis - startMillis
}

object LocalDayWindows {

    fun containing(instantMillis: Long, zone: ZoneId): LocalDayWindow {
        val date = Instant.ofEpochMilli(instantMillis).atZone(zone).toLocalDate()
        return LocalDayWindow(
            startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli(),
            endMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        )
    }
}
