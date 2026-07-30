package com.blocksocial.usage

import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.UsageSession
import java.time.Instant

enum class UsageEventType { MOVED_TO_FOREGROUND, SCREEN_NON_INTERACTIVE }

data class UsageEvent(
    val packageName: String,
    val timestampMillis: Long,
    val type: UsageEventType,
)

object ForegroundSessions {

    fun accumulate(
        events: List<UsageEvent>,
        packageToApp: Map<String, AppRef>,
        windowStartMillis: Long,
        windowEndMillis: Long,
    ): Map<AppRef, List<UsageSession>> {
        val sessions = mutableMapOf<AppRef, MutableList<UsageSession>>()
        var openApp: AppRef? = null
        var openSinceMillis = 0L

        fun close(atMillis: Long, stillRunning: Boolean) {
            val app = openApp ?: return
            openApp = null
            val from = openSinceMillis.coerceAtLeast(windowStartMillis)
            val to = atMillis.coerceAtMost(windowEndMillis)
            if (to > from) {
                sessions.getOrPut(app) { mutableListOf() } += UsageSession(
                    forApp = app,
                    from = Instant.ofEpochMilli(from),
                    to = if (stillRunning) null else Instant.ofEpochMilli(to),
                )
            }
        }

        events
            .filter { it.timestampMillis <= windowEndMillis }
            .sortedBy { it.timestampMillis }
            .forEach { event ->
                when (event.type) {
                    UsageEventType.MOVED_TO_FOREGROUND -> {
                        val app = packageToApp[event.packageName]
                        if (app != openApp) {
                            close(event.timestampMillis, stillRunning = false)
                            openApp = app
                            openSinceMillis = event.timestampMillis
                        }
                    }

                    UsageEventType.SCREEN_NON_INTERACTIVE ->
                        close(event.timestampMillis, stillRunning = false)
                }
            }

        close(windowEndMillis, stillRunning = true)

        return sessions
    }
}
