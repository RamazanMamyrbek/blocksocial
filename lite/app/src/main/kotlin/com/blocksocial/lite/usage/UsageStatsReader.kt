package com.blocksocial.lite.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.os.SystemClock
import java.time.Instant
import java.time.ZoneId

data class UsageToday(
    val minutesByApp: Map<String, Int>,
    val measurementAvailable: Boolean,
) {
    fun minutesFor(app: String): Int? = if (measurementAvailable) minutesByApp[app] ?: 0 else null

    companion object {
        val Unavailable = UsageToday(emptyMap(), measurementAvailable = false)
    }
}

class UsageStatsReader(private val context: Context) {

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun readToday(
        packageToApp: Map<String, String>,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): UsageToday {
        if (!hasUsageAccess()) return UsageToday.Unavailable

        val dayStart = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
        val events = runCatching {
            queryEvents(dayStart.toEpochMilli() - LOOK_BEHIND_MILLIS, now.toEpochMilli())
        }.getOrElse { return UsageToday.Unavailable }

        return UsageToday(
            minutesByApp = ForegroundSessions.minutesByApp(
                events = events,
                packageToApp = packageToApp,
                windowStartMillis = dayStart.toEpochMilli(),
                windowEndMillis = now.toEpochMilli(),
                deviceBootedAtMillis = System.currentTimeMillis() - SystemClock.elapsedRealtime(),
            ),
            measurementAvailable = true,
        )
    }

    private fun queryEvents(fromMillis: Long, untilMillis: Long): List<UsageEvent> {
        val manager = context.getSystemService(UsageStatsManager::class.java) ?: return emptyList()
        val cursor = manager.queryEvents(fromMillis, untilMillis)
        val collected = mutableListOf<UsageEvent>()
        val event = UsageEvents.Event()

        while (cursor.hasNextEvent()) {
            cursor.getNextEvent(event)
            val type = when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> UsageEventType.MOVED_TO_FOREGROUND
                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_STOPPED,
                -> UsageEventType.LEFT_FOREGROUND

                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> UsageEventType.SCREEN_NON_INTERACTIVE
                else -> null
            }
            if (type != null) {
                collected += UsageEvent(
                    packageName = event.packageName.orEmpty(),
                    className = event.className,
                    timestampMillis = event.timeStamp,
                    type = type,
                )
            }
        }
        return collected
    }

    private companion object {
        const val LOOK_BEHIND_MILLIS = 12L * 60 * 60 * 1000
    }
}
