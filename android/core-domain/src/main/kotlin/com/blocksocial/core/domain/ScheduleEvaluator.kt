package com.blocksocial.core.domain

import com.blocksocial.core.model.DeviceTime
import com.blocksocial.core.model.RestrictionRule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ScheduleEvaluator {

    fun isActive(rule: RestrictionRule.Schedule, at: DeviceTime): Boolean {
        if (!rule.enabled) return false
        val localDate = at.wallClock.atZone(at.zone).toLocalDate()
        return candidateStartDays(localDate).any { startDay ->
            startDay.dayOfWeek in rule.daysOfWeek && intervalContains(rule, startDay, at.zone, at.wallClock)
        }
    }

    private fun candidateStartDays(localDate: LocalDate): List<LocalDate> =
        listOf(localDate, localDate.minusDays(1))

    private fun intervalContains(
        rule: RestrictionRule.Schedule,
        startDay: LocalDate,
        zone: ZoneId,
        instant: Instant,
    ): Boolean {
        val start = firstInstantOf(startDay, rule.startLocalTime, zone)
        val endDay =
            if (rule.endLocalTime > rule.startLocalTime) startDay else startDay.plusDays(1)
        val end = firstInstantOf(endDay, rule.endLocalTime, zone)
        return instant >= start && instant < end
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
