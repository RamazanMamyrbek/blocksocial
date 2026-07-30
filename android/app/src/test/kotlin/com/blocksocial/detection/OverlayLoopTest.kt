package com.blocksocial.detection

import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.DeviceTime
import com.blocksocial.core.model.RestrictionRule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class OverlayLoopTest {

    private val instagram = AppRef("instagram")
    private val youtube = AppRef("youtube")

    private val snapshot = ProtectionSnapshot(
        packageToApp = mapOf(
            INSTAGRAM_PACKAGE to instagram,
            YOUTUBE_PACKAGE to youtube,
        ),
        rulesByApp = mapOf(
            instagram to listOf(RestrictionRule.AlwaysOn("instagram-always", enabled = true)),
            youtube to listOf(RestrictionRule.AlwaysOn("youtube-always", enabled = true)),
        ),
        grantsByApp = emptyMap(),
    )

    private fun pipeline() = DetectionPipeline(
        selfPackage = SELF,
        systemPackages = setOf(LAUNCHER, SYSTEM_UI),
        overlayPackages = setOf(SYSTEM_UI),
        isActivityWindow = { packageName, _ -> packageName != SYSTEM_UI },
        readDeviceTime = {
            DeviceTime(
                wallClock = Instant.parse("2026-07-30T10:00:00Z"),
                monotonicMillis = 1_000_000,
                zone = ZoneId.of("Europe/Berlin"),
            )
        },
    )

    private fun DetectionPipeline.enter(packageName: String) =
        onWindowStateChanged(packageName, "$packageName.MainActivity", snapshot)

    @Test
    fun oneLaunchShowsOneBlockHoweverManyWindowsItOpens() {
        val pipeline = pipeline()

        val blocks = listOf(
            pipeline.enter(INSTAGRAM_PACKAGE),
            pipeline.enter(INSTAGRAM_PACKAGE),
            pipeline.enter(INSTAGRAM_PACKAGE),
            pipeline.enter(INSTAGRAM_PACKAGE),
        ).count { it.shouldBlock }

        assertEquals(1, blocks)
    }

    @Test
    fun twentyRapidSwitchesBetweenTwoRestrictedApplicationsBlockOncePerSwitch() {
        val pipeline = pipeline()
        var blocks = 0

        repeat(SWITCHES) {
            if (pipeline.enter(INSTAGRAM_PACKAGE).shouldBlock) blocks++
            if (pipeline.enter(YOUTUBE_PACKAGE).shouldBlock) blocks++
        }

        assertEquals(SWITCHES * 2, blocks)
    }

    @Test
    fun theBlockScreenItselfNeverCountsAsALaunchOfTheApplicationBehindIt() {
        val pipeline = pipeline()
        var blocks = 0

        repeat(SWITCHES) {
            if (pipeline.enter(INSTAGRAM_PACKAGE).shouldBlock) blocks++
            if (pipeline.enter(SELF).shouldBlock) blocks++
        }

        assertEquals(1, blocks)
    }

    @Test
    fun theNotificationShadeOverAnApplicationNeverStartsASecondBlock() {
        val pipeline = pipeline()
        var blocks = 0

        if (pipeline.enter(INSTAGRAM_PACKAGE).shouldBlock) blocks++
        repeat(SWITCHES) {
            if (pipeline.enter(SYSTEM_UI).shouldBlock) blocks++
            if (pipeline.enter(INSTAGRAM_PACKAGE).shouldBlock) blocks++
        }

        assertEquals(1, blocks)
    }

    @Test
    fun goingHomeAndBackIsANewLaunchAndBlocksAgain() {
        val pipeline = pipeline()
        var blocks = 0

        repeat(SWITCHES) {
            if (pipeline.enter(INSTAGRAM_PACKAGE).shouldBlock) blocks++
            if (pipeline.enter(LAUNCHER).shouldBlock) blocks++
        }

        assertEquals(SWITCHES, blocks)
    }

    @Test
    fun theScreenGoingOffMakesTheNextOpenANewLaunch() {
        val pipeline = pipeline()

        assertEquals(true, pipeline.enter(INSTAGRAM_PACKAGE).shouldBlock)
        assertEquals(false, pipeline.enter(INSTAGRAM_PACKAGE).shouldBlock)

        pipeline.forgetForeground()

        assertEquals(true, pipeline.enter(INSTAGRAM_PACKAGE).shouldBlock)
    }

    @Test
    fun aStormOfEventsFromEverySourceStillBlocksOncePerRealLaunch() {
        val pipeline = pipeline()
        var blocks = 0

        repeat(SWITCHES) {
            if (pipeline.enter(INSTAGRAM_PACKAGE).shouldBlock) blocks++
            if (pipeline.enter(SELF).shouldBlock) blocks++
            if (pipeline.enter(SYSTEM_UI).shouldBlock) blocks++
            if (pipeline.enter(INSTAGRAM_PACKAGE).shouldBlock) blocks++
            if (pipeline.enter(LAUNCHER).shouldBlock) blocks++
        }

        assertEquals(SWITCHES, blocks)
    }

    private companion object {
        const val SELF = "com.blocksocial"
        const val LAUNCHER = "com.android.launcher"
        const val SYSTEM_UI = "com.android.systemui"
        const val INSTAGRAM_PACKAGE = "com.instagram.android"
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        const val SWITCHES = 20
    }
}
