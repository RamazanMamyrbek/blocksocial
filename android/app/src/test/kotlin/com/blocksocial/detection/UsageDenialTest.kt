package com.blocksocial.detection

import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.DeviceTime
import com.blocksocial.core.model.RestrictionRule
import com.blocksocial.core.model.RuleMode
import com.blocksocial.core.model.UsageSession
import com.blocksocial.usage.UsageReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

class UsageDenialTest {

    private val youtube = AppRef("youtube")
    private val zone = ZoneId.of("Asia/Tokyo")

    private val mondayEvening = Instant.parse("2026-07-27T19:30:00+09:00")

    private fun pipeline() = DetectionPipeline(
        selfPackage = "com.blocksocial",
        systemPackages = setOf("com.android.settings"),
        isActivityWindow = { _, _ -> true },
        readDeviceTime = { DeviceTime(mondayEvening, 5_000_000, zone) },
    )

    private fun snapshot(rules: List<RestrictionRule>) = ProtectionSnapshot(
        packageToApp = mapOf("com.google.android.youtube" to youtube),
        rulesByApp = mapOf(youtube to rules),
        grantsByApp = emptyMap(),
    )

    private val eveningSchedule = RestrictionRule.Schedule(
        id = "evening",
        enabled = true,
        daysOfWeek = setOf(DayOfWeek.MONDAY),
        startLocalTime = LocalTime.of(18, 0),
        endLocalTime = LocalTime.of(21, 0),
    )

    private val tightLimit = RestrictionRule.DailyLimit("limit", enabled = true, limitMinutes = 10)

    @Test
    fun aScheduleStillBlocksWhenUsageAccessIsDenied() {
        val result = pipeline().onWindowStateChanged(
            packageName = "com.google.android.youtube",
            className = "Main",
            snapshot = snapshot(listOf(eveningSchedule)),
            readUsage = { UsageReading.Unavailable },
        )

        assertTrue(result.shouldBlock)
        assertEquals(RuleMode.SCHEDULE, result.decision?.primaryReason)
    }

    @Test
    fun aDailyLimitCannotBlockWhenUsageAccessIsDenied() {
        val result = pipeline().onWindowStateChanged(
            packageName = "com.google.android.youtube",
            className = "Main",
            snapshot = snapshot(listOf(tightLimit)),
            readUsage = { UsageReading.Unavailable },
        )

        assertFalse(result.shouldBlock)
    }

    @Test
    fun denyingUsageAccessRemovesOnlyTheLimitReason() {
        val both = snapshot(listOf(eveningSchedule, tightLimit))
        val measured = UsageReading(
            sessionsByApp = mapOf(
                youtube to listOf(
                    UsageSession(
                        forApp = youtube,
                        from = Instant.parse("2026-07-27T19:00:00+09:00"),
                        to = Instant.parse("2026-07-27T19:20:00+09:00"),
                    ),
                ),
            ),
            measurementAvailable = true,
        )

        val withUsage = pipeline().onWindowStateChanged("com.google.android.youtube", "Main", both) { measured }
        val withoutUsage = pipeline().onWindowStateChanged("com.google.android.youtube", "Main", both) { UsageReading.Unavailable }

        assertEquals(listOf(RuleMode.SCHEDULE, RuleMode.DAILY_LIMIT), withUsage.decision?.allReasons)
        assertEquals(listOf(RuleMode.SCHEDULE), withoutUsage.decision?.allReasons)
        assertTrue(withoutUsage.shouldBlock)
    }

    @Test
    fun noMinuteCountIsReportedWhenThereIsNothingToMeasureWith() {
        val result = pipeline().onWindowStateChanged(
            packageName = "com.google.android.youtube",
            className = "Main",
            snapshot = snapshot(listOf(tightLimit)),
            readUsage = { UsageReading.Unavailable },
        )

        assertNull(result.decision?.dailyLimit?.measuredMinutesToday)
    }

    @Test
    fun theMeasurementIsReadOnlyWhenAVisitToARestrictedApplicationBegins() {
        var reads = 0
        val readUsage = { reads++; UsageReading.Unavailable }
        val pipeline = pipeline()
        val restricted = snapshot(listOf(tightLimit))

        pipeline.onWindowStateChanged("com.android.settings", "Main", restricted, readUsage)
        pipeline.onWindowStateChanged("com.android.chrome", "Main", restricted, readUsage)
        assertEquals(0, reads)

        pipeline.onWindowStateChanged("com.google.android.youtube", "Main", restricted, readUsage)
        pipeline.onWindowStateChanged("com.google.android.youtube", "Main", restricted, readUsage)
        assertEquals(1, reads)
    }

    @Test
    fun aReachedLimitBlocksOnceMeasurementIsAvailable() {
        val measured = UsageReading(
            sessionsByApp = mapOf(
                youtube to listOf(
                    UsageSession(
                        forApp = youtube,
                        from = Instant.parse("2026-07-27T09:00:00+09:00"),
                        to = Instant.parse("2026-07-27T09:30:00+09:00"),
                    ),
                ),
            ),
            measurementAvailable = true,
        )

        val result = pipeline().onWindowStateChanged(
            packageName = "com.google.android.youtube",
            className = "Main",
            snapshot = snapshot(listOf(tightLimit)),
            readUsage = { measured },
        )

        assertTrue(result.shouldBlock)
        assertEquals(RuleMode.DAILY_LIMIT, result.decision?.primaryReason)
    }
}
