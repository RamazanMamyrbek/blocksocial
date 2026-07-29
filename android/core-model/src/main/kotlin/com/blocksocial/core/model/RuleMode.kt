package com.blocksocial.core.model

enum class RuleMode(val priority: Int) {
    ALWAYS(0),
    FOCUS_SESSION(1),
    SCHEDULE(2),
    DAILY_LIMIT(3),
}
