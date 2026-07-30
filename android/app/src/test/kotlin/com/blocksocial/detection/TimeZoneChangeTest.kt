package com.blocksocial.detection

import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.DeviceTime
import com.blocksocial.core.model.RestrictionRule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

class TimeZoneChangeTest {

    private val youtube = AppRef("youtube")

    private val overnight = RestrictionRule.Schedule(
        id = "overnight",
        enabled = true,
        daysOfWeek = DayOfWeek.entries.toSet(),
        startLocalTime = LocalTime.of(22, 0),
        endLocalTime = LocalTime.of(7, 0),
    )

    private val snapshot = ProtectionSnapshot(
        packageToApp = mapOf(PACKAGE to youtube),
        rulesByApp = mapOf(youtube to listOf(overnight)),
        grantsByApp = emptyMap(),
    )

    private var zone: ZoneId = ZoneId.of("Europe/London")

    private val pipeline = DetectionPipeline(
        selfPackage = "com.blocksocial",
        systemPackages = setOf(LAUNCHER),
        isActivityWindow = { _, _ -> true },
        readDeviceTime = {
            DeviceTime(wallClock = INSTANT, monotonicMillis = 1_000, zone = zone)
        },
    )

    private fun launchYoutube(): Boolean {
        pipeline.onWindowStateChanged(LAUNCHER, "$LAUNCHER.Home", snapshot)
        return pipeline.onWindowStateChanged(PACKAGE, "$PACKAGE.Main", snapshot).shouldBlock
    }

    @Test
    fun theSameInstantIsInsideTheWindowInOneZoneAndOutsideItInAnother() {
        zone = ZoneId.of("Europe/London")
        assertEquals("18:50 London is outside 22:00-07:00", false, launchYoutube())

        zone = ZoneId.of("Asia/Tokyo")
        assertEquals("02:50 Tokyo is inside 22:00-07:00", true, launchYoutube())
    }

    @Test
    fun aZoneChangeTakesEffectOnTheVeryNextLaunchWithNothingRestarted() {
        zone = ZoneId.of("Asia/Tokyo")
        assertEquals(true, launchYoutube())

        zone = ZoneId.of("Europe/London")
        assertEquals("the old zone was still being used", false, launchYoutube())

        zone = ZoneId.of("Asia/Tokyo")
        assertEquals("the zone is read once and cached", true, launchYoutube())
    }

    @Test
    fun theRuleFollowsLocalTimeAcrossADaylightSavingTransition() {
        val berlin = ZoneId.of("Europe/Berlin")
        val beforeTheGap = Instant.parse("2027-03-28T00:30:00Z")
        val afterTheGap = Instant.parse("2027-03-28T01:30:00Z")

        assertEquals("01:30 Berlin is inside 22:00-07:00", true, blocksAt(beforeTheGap, berlin))
        assertEquals("03:30 Berlin is inside 22:00-07:00", true, blocksAt(afterTheGap, berlin))
        assertEquals(
            "09:30 Berlin is outside 22:00-07:00",
            false,
            blocksAt(Instant.parse("2027-03-28T07:30:00Z"), berlin),
        )
    }

    private fun blocksAt(instant: Instant, at: ZoneId): Boolean {
        val fresh = DetectionPipeline(
            selfPackage = "com.blocksocial",
            systemPackages = setOf(LAUNCHER),
            isActivityWindow = { _, _ -> true },
            readDeviceTime = { DeviceTime(wallClock = instant, monotonicMillis = 1_000, zone = at) },
        )
        return fresh.onWindowStateChanged(PACKAGE, "$PACKAGE.Main", snapshot).shouldBlock
    }

    private companion object {
        const val PACKAGE = "com.google.android.youtube"
        const val LAUNCHER = "com.android.launcher"
        val INSTANT: Instant = Instant.parse("2026-07-30T17:50:00Z")
    }
}
