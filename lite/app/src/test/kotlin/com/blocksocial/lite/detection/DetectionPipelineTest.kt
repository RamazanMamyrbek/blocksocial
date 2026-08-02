package com.blocksocial.lite.detection

import com.blocksocial.lite.usage.UsageToday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionPipelineTest {

    private val youtube = "youtube"
    private val instagram = "instagram"

    private fun pipeline() = DetectionPipeline(
        selfPackage = "com.blocksocial.lite",
        systemPackages = setOf("com.android.settings", "com.android.launcher"),
        isActivityWindow = { _, _ -> true },
    )

    private fun snapshot(limits: Map<String, Int>) = LimitSnapshot(
        packageToApp = mapOf(
            "com.google.android.youtube" to youtube,
            "com.instagram.android" to instagram,
        ),
        displayNames = mapOf(youtube to "YouTube", instagram to "Instagram"),
        limits = limits,
    )

    private fun used(minutes: Map<String, Int>) = { UsageToday(minutes, measurementAvailable = true) }

    @Test
    fun anApplicationOverItsLimitIsWarnedAbout() {
        val result = pipeline().onWindowStateChanged(
            "com.google.android.youtube",
            "Main",
            snapshot(mapOf(youtube to 30)),
            used(mapOf(youtube to 47)),
        )

        assertEquals(TransitionOutcome.TARGET_ENTERED, result.transition)
        assertTrue(result.shouldWarn)
        assertEquals(47, result.status?.usedMinutes)
        assertEquals(30, result.status?.limitMinutes)
    }

    @Test
    fun anApplicationUnderItsLimitIsLeftAlone() {
        val result = pipeline().onWindowStateChanged(
            "com.google.android.youtube",
            "Main",
            snapshot(mapOf(youtube to 30)),
            used(mapOf(youtube to 12)),
        )

        assertFalse(result.shouldWarn)
        assertEquals(18, result.status?.remainingMinutes)
    }

    @Test
    fun anApplicationWithoutALimitIsNotEvenTracked() {
        val result = pipeline().onWindowStateChanged(
            "com.instagram.android",
            "Main",
            snapshot(mapOf(youtube to 30)),
            used(mapOf(instagram to 200)),
        )

        assertEquals(TransitionOutcome.IGNORED_NOT_A_TARGET, result.transition)
        assertNull(result.status)
    }

    @Test
    fun withoutAMeasurementNothingIsWarnedAbout() {
        val result = pipeline().onWindowStateChanged(
            "com.google.android.youtube",
            "Main",
            snapshot(mapOf(youtube to 5)),
            { UsageToday.Unavailable },
        )

        assertFalse(result.shouldWarn)
        assertFalse(result.status!!.measured)
    }

    @Test
    fun theWarningIsRaisedAgainOnEveryFreshEntry() {
        val pipeline = pipeline()
        val over = snapshot(mapOf(youtube to 30))

        val first = pipeline.onWindowStateChanged("com.google.android.youtube", "Main", over, used(mapOf(youtube to 47)))
        val staying = pipeline.onWindowStateChanged("com.google.android.youtube", "Main", over, used(mapOf(youtube to 47)))
        pipeline.onWindowStateChanged("com.android.launcher", "Home", over, used(mapOf(youtube to 47)))
        val second = pipeline.onWindowStateChanged("com.google.android.youtube", "Main", over, used(mapOf(youtube to 47)))

        assertTrue(first.shouldWarn)
        assertEquals(TransitionOutcome.IGNORED_ALREADY_FOREGROUND, staying.transition)
        assertFalse(staying.shouldWarn)
        assertTrue(second.shouldWarn)
    }

    @Test
    fun theMeasurementIsReadOnlyWhenAVisitToALimitedApplicationBegins() {
        var reads = 0
        val readUsage = { reads++; UsageToday.Unavailable }
        val pipeline = pipeline()
        val limited = snapshot(mapOf(youtube to 30))

        pipeline.onWindowStateChanged("com.android.settings", "Main", limited, readUsage)
        pipeline.onWindowStateChanged("com.instagram.android", "Main", limited, readUsage)
        assertEquals(0, reads)

        pipeline.onWindowStateChanged("com.google.android.youtube", "Main", limited, readUsage)
        pipeline.onWindowStateChanged("com.google.android.youtube", "Main", limited, readUsage)
        assertEquals(1, reads)
    }

    @Test
    fun blockSocialLiteNeverWarnsAboutItself() {
        val result = pipeline().onWindowStateChanged(
            "com.blocksocial.lite",
            "Main",
            snapshot(mapOf(youtube to 30)),
            used(mapOf(youtube to 47)),
        )

        assertEquals(TransitionOutcome.IGNORED_SELF, result.transition)
    }

    @Test
    fun aCriticalSystemApplicationIsNeverWarnedAbout() {
        val result = pipeline().onWindowStateChanged(
            "com.android.settings",
            "Main",
            snapshot(mapOf(youtube to 30)),
            used(mapOf(youtube to 47)),
        )

        assertEquals(TransitionOutcome.IGNORED_SYSTEM, result.transition)
    }
}
