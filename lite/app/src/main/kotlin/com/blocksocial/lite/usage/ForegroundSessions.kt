package com.blocksocial.lite.usage

enum class UsageEventType { MOVED_TO_FOREGROUND, LEFT_FOREGROUND, SCREEN_NON_INTERACTIVE }

data class UsageEvent(
    val packageName: String,
    val timestampMillis: Long,
    val type: UsageEventType,
)

object ForegroundSessions {

    fun minutesByApp(
        events: List<UsageEvent>,
        packageToApp: Map<String, String>,
        windowStartMillis: Long,
        windowEndMillis: Long,
        deviceBootedAtMillis: Long,
    ): Map<String, Int> {
        val millisByApp = mutableMapOf<String, Long>()
        val earliestCountableMillis = maxOf(windowStartMillis, deviceBootedAtMillis)
        var openApp: String? = null
        var openSinceMillis = 0L

        fun close(atMillis: Long) {
            val app = openApp ?: return
            openApp = null
            val from = openSinceMillis.coerceAtLeast(earliestCountableMillis)
            val to = atMillis.coerceAtMost(windowEndMillis)
            if (to > from) millisByApp[app] = (millisByApp[app] ?: 0L) + (to - from)
        }

        events
            .filter { it.timestampMillis <= windowEndMillis }
            .sortedBy { it.timestampMillis }
            .forEach { event ->
                when (event.type) {
                    UsageEventType.MOVED_TO_FOREGROUND -> {
                        val app = packageToApp[event.packageName]
                        if (app != openApp) {
                            close(event.timestampMillis)
                            openApp = app
                            openSinceMillis = event.timestampMillis
                        }
                    }

                    UsageEventType.LEFT_FOREGROUND ->
                        if (packageToApp[event.packageName] == openApp) close(event.timestampMillis)

                    UsageEventType.SCREEN_NON_INTERACTIVE -> close(event.timestampMillis)
                }
            }

        close(windowEndMillis)

        return millisByApp.mapValues { (_, millis) -> (millis / MILLIS_PER_MINUTE).toInt() }
    }

    private const val MILLIS_PER_MINUTE = 60_000L
}
