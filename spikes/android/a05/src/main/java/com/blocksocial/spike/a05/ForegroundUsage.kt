package com.blocksocial.spike.a05

enum class UsageEventType {
    MOVED_TO_FOREGROUND,
    SCREEN_NON_INTERACTIVE
}

data class UsageEvent(
    val packageName: String,
    val timestampMillis: Long,
    val type: UsageEventType
)

enum class UsageConfidence {
    MEASURED,
    IN_PROGRESS
}

data class ForegroundUsage(
    val packageName: String,
    val totalMillis: Long,
    val confidence: UsageConfidence,
    val sessionCount: Int
)

object ForegroundTimeAccumulator {

    fun accumulate(
        events: List<UsageEvent>,
        windowStartMillis: Long,
        windowEndMillis: Long
    ): Map<String, ForegroundUsage> {
        val totals = mutableMapOf<String, Long>()
        val sessions = mutableMapOf<String, Int>()
        var openPackage: String? = null
        var openSince = 0L

        fun closeOpenSession(atMillis: Long) {
            val packageName = openPackage ?: return
            openPackage = null
            val from = openSince.coerceAtLeast(windowStartMillis)
            val to = atMillis.coerceAtMost(windowEndMillis)
            if (to > from) {
                totals[packageName] = (totals[packageName] ?: 0L) + (to - from)
                sessions[packageName] = (sessions[packageName] ?: 0) + 1
            }
        }

        events
            .filter { it.timestampMillis <= windowEndMillis }
            .sortedBy { it.timestampMillis }
            .forEach { event ->
                when (event.type) {
                    UsageEventType.MOVED_TO_FOREGROUND -> {
                        if (event.packageName != openPackage) {
                            closeOpenSession(event.timestampMillis)
                            openPackage = event.packageName
                            openSince = event.timestampMillis
                        }
                    }

                    UsageEventType.SCREEN_NON_INTERACTIVE -> closeOpenSession(event.timestampMillis)
                }
            }

        val stillOpen = openPackage
        closeOpenSession(windowEndMillis)

        return totals.keys.associateWith { packageName ->
            ForegroundUsage(
                packageName = packageName,
                totalMillis = totals.getValue(packageName),
                confidence = if (packageName == stillOpen) {
                    UsageConfidence.IN_PROGRESS
                } else {
                    UsageConfidence.MEASURED
                },
                sessionCount = sessions[packageName] ?: 0
            )
        }
    }
}
