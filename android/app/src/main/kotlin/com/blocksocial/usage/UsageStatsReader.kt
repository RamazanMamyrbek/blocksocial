package com.blocksocial.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.os.SystemClock
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.UsageSession
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class UsageReading(
    val sessionsByApp: Map<AppRef, List<UsageSession>>,
    val measurementAvailable: Boolean,
) {
    companion object {
        val Unavailable = UsageReading(emptyMap(), measurementAvailable = false)
    }
}

@Singleton
class UsageStatsReader @Inject constructor(@ApplicationContext private val context: Context) {

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

    fun readToday(packageToApp: Map<String, AppRef>, now: Instant, zone: ZoneId): UsageReading {
        if (!hasUsageAccess()) return UsageReading.Unavailable

        val dayStart = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
        val events = runCatching {
            queryEvents(dayStart.toEpochMilli() - LOOK_BEHIND_MILLIS, now.toEpochMilli())
        }.getOrElse { return UsageReading.Unavailable }

        return UsageReading(
            sessionsByApp = ForegroundSessions.accumulate(
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
        val manager = context.getSystemService(UsageStatsManager::class.java)
            ?: return emptyList()
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
