package com.blocksocial.spike.a05

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

class UsageStatsReader(private val usageStatsManager: UsageStatsManager) {

    fun readEvents(windowStartMillis: Long, windowEndMillis: Long): List<UsageEvent> {
        val collected = mutableListOf<UsageEvent>()
        val stream = usageStatsManager.queryEvents(windowStartMillis, windowEndMillis)
        val event = UsageEvents.Event()
        while (stream.hasNextEvent()) {
            stream.getNextEvent(event)
            val type = when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> UsageEventType.MOVED_TO_FOREGROUND
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> UsageEventType.SCREEN_NON_INTERACTIVE
                else -> null
            }
            val packageName = event.packageName
            if (type != null && packageName != null) {
                collected += UsageEvent(packageName, event.timeStamp, type)
            }
        }
        return collected
    }

    fun readBucketedForegroundMillis(
        windowStartMillis: Long,
        windowEndMillis: Long
    ): Map<String, Long> {
        val buckets = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            windowStartMillis,
            windowEndMillis
        ) ?: return emptyMap()
        val totals = mutableMapOf<String, Long>()
        buckets.forEach { stats ->
            val current = totals[stats.packageName] ?: 0L
            totals[stats.packageName] = maxOf(current, stats.totalTimeInForeground)
        }
        return totals
    }

    fun readRawEventTypes(
        windowStartMillis: Long,
        windowEndMillis: Long,
        packageName: String
    ): List<Pair<Long, Int>> {
        val collected = mutableListOf<Pair<Long, Int>>()
        val stream = usageStatsManager.queryEvents(windowStartMillis, windowEndMillis)
        val event = UsageEvents.Event()
        while (stream.hasNextEvent()) {
            stream.getNextEvent(event)
            if (event.packageName == packageName) {
                collected += event.timeStamp to event.eventType
            }
        }
        return collected
    }

    companion object {
        fun from(context: Context) =
            UsageStatsReader(context.getSystemService(UsageStatsManager::class.java))
    }
}
