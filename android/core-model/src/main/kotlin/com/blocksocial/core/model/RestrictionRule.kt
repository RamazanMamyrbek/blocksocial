package com.blocksocial.core.model

import java.time.DayOfWeek
import java.time.LocalTime

sealed interface RestrictionRule {
    val id: String
    val enabled: Boolean
    val mode: RuleMode

    data class AlwaysOn(
        override val id: String,
        override val enabled: Boolean,
    ) : RestrictionRule {
        override val mode: RuleMode = RuleMode.ALWAYS
    }

    data class FocusSession(
        override val id: String,
        override val enabled: Boolean,
    ) : RestrictionRule {
        override val mode: RuleMode = RuleMode.FOCUS_SESSION
    }

    data class Schedule(
        override val id: String,
        override val enabled: Boolean,
        val daysOfWeek: Set<DayOfWeek>,
        val startLocalTime: LocalTime,
        val endLocalTime: LocalTime,
    ) : RestrictionRule {
        override val mode: RuleMode = RuleMode.SCHEDULE
    }

    data class DailyLimit(
        override val id: String,
        override val enabled: Boolean,
        val limitMinutes: Int,
    ) : RestrictionRule {
        override val mode: RuleMode = RuleMode.DAILY_LIMIT
    }
}
