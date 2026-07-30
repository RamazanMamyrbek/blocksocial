package com.blocksocial.feature.rules

import com.blocksocial.core.model.RestrictionRule
import com.blocksocial.core.model.RuleMode
import java.time.DayOfWeek
import java.time.LocalTime

enum class RuleProblem { NO_DAYS_SELECTED, INTERVAL_IS_EMPTY, LIMIT_OUT_OF_RANGE }

data class IntervalDescription(
    val startLocalTime: LocalTime,
    val endLocalTime: LocalTime,
) {
    val crossesMidnight: Boolean = endLocalTime <= startLocalTime

    private val minutesBeforeMidnight: Int =
        if (crossesMidnight) MINUTES_PER_DAY - startLocalTime.toMinutes() else 0

    val firstSegmentMinutes: Int =
        if (crossesMidnight) minutesBeforeMidnight else endLocalTime.toMinutes() - startLocalTime.toMinutes()

    val secondSegmentMinutes: Int = if (crossesMidnight) endLocalTime.toMinutes() else 0

    val totalMinutes: Int = firstSegmentMinutes + secondSegmentMinutes

    private fun LocalTime.toMinutes(): Int = hour * 60 + minute

    private companion object {
        const val MINUTES_PER_DAY = 24 * 60
    }
}

data class RuleDraft(
    val id: String,
    val mode: RuleMode,
    val enabled: Boolean = true,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val startLocalTime: LocalTime = LocalTime.of(22, 0),
    val endLocalTime: LocalTime = LocalTime.of(7, 0),
    val dailyLimitMinutes: Int = 30,
) {

    val interval: IntervalDescription = IntervalDescription(startLocalTime, endLocalTime)

    fun problems(): List<RuleProblem> = when (mode) {
        RuleMode.SCHEDULE -> buildList {
            if (daysOfWeek.isEmpty()) add(RuleProblem.NO_DAYS_SELECTED)
            if (startLocalTime == endLocalTime) add(RuleProblem.INTERVAL_IS_EMPTY)
        }
        RuleMode.DAILY_LIMIT ->
            if (dailyLimitMinutes !in MINIMUM_LIMIT_MINUTES..MAXIMUM_LIMIT_MINUTES) {
                listOf(RuleProblem.LIMIT_OUT_OF_RANGE)
            } else {
                emptyList()
            }
        RuleMode.ALWAYS, RuleMode.FOCUS_SESSION -> emptyList()
    }

    val isValid: Boolean get() = problems().isEmpty()

    fun toRule(): RestrictionRule? {
        if (!isValid) return null
        return when (mode) {
            RuleMode.ALWAYS -> RestrictionRule.AlwaysOn(id = id, enabled = enabled)
            RuleMode.FOCUS_SESSION -> RestrictionRule.FocusSession(id = id, enabled = enabled)
            RuleMode.SCHEDULE -> RestrictionRule.Schedule(
                id = id,
                enabled = enabled,
                daysOfWeek = daysOfWeek,
                startLocalTime = startLocalTime,
                endLocalTime = endLocalTime,
            )
            RuleMode.DAILY_LIMIT -> RestrictionRule.DailyLimit(
                id = id,
                enabled = enabled,
                limitMinutes = dailyLimitMinutes,
            )
        }
    }

    companion object {
        const val MINIMUM_LIMIT_MINUTES = 5
        const val MAXIMUM_LIMIT_MINUTES = 12 * 60

        fun of(rule: RestrictionRule): RuleDraft = when (rule) {
            is RestrictionRule.AlwaysOn -> RuleDraft(rule.id, RuleMode.ALWAYS, rule.enabled)
            is RestrictionRule.FocusSession -> RuleDraft(rule.id, RuleMode.FOCUS_SESSION, rule.enabled)
            is RestrictionRule.Schedule -> RuleDraft(
                id = rule.id,
                mode = RuleMode.SCHEDULE,
                enabled = rule.enabled,
                daysOfWeek = rule.daysOfWeek,
                startLocalTime = rule.startLocalTime,
                endLocalTime = rule.endLocalTime,
            )
            is RestrictionRule.DailyLimit -> RuleDraft(
                id = rule.id,
                mode = RuleMode.DAILY_LIMIT,
                enabled = rule.enabled,
                dailyLimitMinutes = rule.limitMinutes,
            )
        }
    }
}
