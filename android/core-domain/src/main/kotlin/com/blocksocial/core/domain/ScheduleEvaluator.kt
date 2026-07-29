package com.blocksocial.core.domain

import com.blocksocial.core.model.DeviceTime
import com.blocksocial.core.model.RestrictionRule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ScheduleEvaluator {

    fun isActive(rule: RestrictionRule.Schedule, at: DeviceTime): Boolean =
        activeInterval(rule, at) != null

    fun activeUntil(rule: RestrictionRule.Schedule, at: DeviceTime): Instant? =
        activeInterval(rule, at)?.endExclusive

    private fun activeInterval(rule: RestrictionRule.Schedule, at: DeviceTime): Interval? {
        if (!rule.enabled) return null
        val localDate = at.wallClock.atZone(at.zone).toLocalDate()
        return candidateStartDays(localDate)
            .filter { it.dayOfWeek in rule.daysOfWeek }
            .map { startDay -> intervalOf(rule, startDay, at.zone) }
            .firstOrNull { at.wallClock >= it.start && at.wallClock < it.endExclusive }
    }

    private data class Interval(val start: Instant, val endExclusive: Instant)

    private fun candidateStartDays(localDate: LocalDate): List<LocalDate> =
        listOf(localDate, localDate.minusDays(1))

    private fun intervalOf(
        rule: RestrictionRule.Schedule,
        startDay: LocalDate,
        zone: ZoneId,
    ): Interval {
        val endDay =
            if (rule.endLocalTime > rule.startLocalTime) startDay else startDay.plusDays(1)
        return Interval(
            start = firstInstantOf(startDay, rule.startLocalTime, zone),
            endExclusive = firstInstantOf(endDay, rule.endLocalTime, zone),
        )
    }

    private fun firstInstantOf(date: LocalDate, time: LocalTime, zone: ZoneId): Instant {
        val localDateTime = LocalDateTime.of(date, time)
        val transition = zone.rules.getTransition(localDateTime)
        return if (transition != null && transition.isGap) {
            transition.instant
        } else {
            localDateTime.atZone(zone).toInstant()
        }
    }
}
