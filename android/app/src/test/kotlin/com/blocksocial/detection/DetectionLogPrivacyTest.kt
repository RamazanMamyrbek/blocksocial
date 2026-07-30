package com.blocksocial.detection

import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.DeviceTime
import com.blocksocial.core.model.RestrictionRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class DetectionLogPrivacyTest {

    private val youtube = AppRef("youtube")

    private val snapshot = ProtectionSnapshot(
        packageToApp = mapOf(PACKAGE to youtube),
        rulesByApp = mapOf(youtube to listOf(RestrictionRule.AlwaysOn("always", enabled = true))),
        grantsByApp = emptyMap(),
        displayNames = mapOf(youtube to DISPLAY_NAME),
    )

    private fun line(packageName: String, className: String): String {
        val pipeline = DetectionPipeline(
            selfPackage = "com.blocksocial",
            systemPackages = emptySet(),
            isActivityWindow = { _, _ -> true },
            readDeviceTime = {
                DeviceTime(
                    wallClock = Instant.parse("2026-07-30T12:00:00Z"),
                    monotonicMillis = 1_000,
                    zone = ZoneId.of("Europe/Berlin"),
                )
            },
        )
        val result = pipeline.onWindowStateChanged(packageName, className, snapshot)
        return DetectionLog.decisionLine(result, latencyMillis = 12)
    }

    @Test
    fun theLogNamesTheCatalogEntryAndNeverThePackageOrTheWindow() {
        val logged = line(PACKAGE, WINDOW_TITLE)

        assertTrue("the catalog identifier is missing", logged.contains("app=youtube"))
        assertFalse("the package name reached the log", logged.contains(PACKAGE))
        assertFalse("a window title reached the log", logged.contains(WINDOW_TITLE))
        assertFalse("a display name reached the log", logged.contains(DISPLAY_NAME))
    }

    @Test
    fun everyLoggedValueIsAKeyedFieldWithNoFreeText() {
        val fields = line(PACKAGE, WINDOW_TITLE).split(' ')

        fields.forEach { field ->
            assertTrue("\"$field\" is not a key=value field", field.matches(FIELD))
        }
    }

    @Test
    fun anUnknownApplicationIsLoggedAsNoneRatherThanByName() {
        val logged = line(SOMEONE_ELSE, WINDOW_TITLE)

        assertTrue(logged.contains("app=none"))
        assertFalse("an unrestricted package reached the log", logged.contains(SOMEONE_ELSE))
    }

    private companion object {
        const val PACKAGE = "com.google.android.youtube"
        const val SOMEONE_ELSE = "com.somebody.private.diary"
        const val WINDOW_TITLE = "com.google.android.youtube.WatchWhileActivity"
        const val DISPLAY_NAME = "YouTube"
        val FIELD = Regex("[a-zA-Z]+=[A-Za-z0-9_|]+")
    }
}
