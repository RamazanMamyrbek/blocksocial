package com.blocksocial.lite.domain

data class LimitStatus(
    val limitMinutes: Int,
    val usedMinutes: Int?,
) {
    val measured: Boolean = usedMinutes != null

    val remainingMinutes: Int? = usedMinutes?.let { (limitMinutes - it).coerceAtLeast(0) }

    val reached: Boolean = usedMinutes != null && usedMinutes >= limitMinutes
}

object DailyLimit {

    fun statusOf(limitMinutes: Int?, usedMinutes: Int?): LimitStatus? =
        limitMinutes?.let { LimitStatus(limitMinutes = it, usedMinutes = usedMinutes) }
}
