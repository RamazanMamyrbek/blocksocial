package com.blocksocial.core.model

import java.time.Instant
import java.time.ZoneId

enum class UserAction { STAYED_FOCUSED, BYPASSED, DISMISSED_BY_SYSTEM, UNKNOWN }

enum class Platform { ANDROID, IOS }

data class BlockEvent(
    val id: String,
    val restrictedAppRef: AppRef,
    val occurredAt: Instant,
    val zone: ZoneId,
    val primaryReason: RuleMode,
    val allReasons: List<RuleMode>,
    val userAction: UserAction,
    val bypassDurationMinutes: Int?,
    val platform: Platform,
    val eventSchemaVersion: Int = EVENT_SCHEMA_VERSION,
) {
    init {
        require(primaryReason in allReasons) {
            "primaryReason $primaryReason is missing from allReasons $allReasons"
        }
        require(allReasons == allReasons.sortedBy { it.priority }) {
            "allReasons $allReasons is not in priority order"
        }
        require((userAction == UserAction.BYPASSED) == (bypassDurationMinutes != null)) {
            "bypassDurationMinutes must be present exactly when userAction is BYPASSED"
        }
    }

    companion object {
        const val EVENT_SCHEMA_VERSION = 1
    }
}
