package com.blocksocial.core.domain

import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.DeviceTime
import com.blocksocial.core.model.RestrictionRule
import com.blocksocial.core.model.UsageSession
import java.time.Duration
import java.time.Instant

enum class MeasurementState { SETTLED, IN_PROGRESS, UNAVAILABLE }

data class DailyLimitDecision(
    val measuredMinutesToday: Int?,
    val restrictionActive: Boolean,
    val measurementState: MeasurementState,
    val blockPresentedImmediately: Boolean,
)

object DailyLimitEvaluator {

    private const val BLOCK_APPEARS_ON_THE_NEXT_ENTRY = true

    fun evaluate(
        rule: RestrictionRule.DailyLimit,
        sessions: List<UsageSession>,
        forApp: AppRef,
        at: DeviceTime,
        usageMeasurementAvailable: Boolean,
    ): DailyLimitDecision {
        if (!usageMeasurementAvailable) {
            return DailyLimitDecision(
                measuredMinutesToday = null,
                restrictionActive = false,
                measurementState = MeasurementState.UNAVAILABLE,
                blockPresentedImmediately = false,
            )
        }

        val dayStart = startOfLocalDay(at)
        var measuredMillis = 0L
        var stillRunning = false

        sessions.filter { it.forApp == forApp }.forEach { session ->
            val from = maxOf(session.from, dayStart)
            val until = minOf(session.to ?: at.wallClock, at.wallClock)
            if (until > from) {
                measuredMillis += Duration.between(from, until).toMillis()
                if (session.to == null) stillRunning = true
            }
        }

        val measuredMinutes = (measuredMillis / MILLIS_PER_MINUTE).toInt()
        val restrictionActive = rule.enabled && measuredMinutes >= rule.limitMinutes

        return DailyLimitDecision(
            measuredMinutesToday = measuredMinutes,
            restrictionActive = restrictionActive,
            measurementState =
                if (stillRunning) MeasurementState.IN_PROGRESS else MeasurementState.SETTLED,
            blockPresentedImmediately = restrictionActive && !BLOCK_APPEARS_ON_THE_NEXT_ENTRY,
        )
    }

    private fun startOfLocalDay(at: DeviceTime): Instant =
        at.wallClock.atZone(at.zone).toLocalDate().atStartOfDay(at.zone).toInstant()

    private const val MILLIS_PER_MINUTE = 60_000L
}
