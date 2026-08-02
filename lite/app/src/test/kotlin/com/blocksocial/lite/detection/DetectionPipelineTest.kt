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
        val spent = used(mapOf(youtube to 47))

        val first = pipeline.onWindowStateChanged("com.google.android.youtube", over, spent)
        val staying = pipeline.onWindowStateChanged("com.google.android.youtube", over, spent)
        pipeline.onWindowStateChanged("com.android.launcher", over, spent)
        val second = pipeline.onWindowStateChanged("com.google.android.youtube", over, spent)
        pipeline.onWindowStateChanged("com.instagram.android", over, spent)
        val third = pipeline.onWindowStateChanged("com.google.android.youtube", over, spent)

        assertTrue(first.shouldWarn)
        assertEquals(TransitionOutcome.IGNORED_ALREADY_FOREGROUND, staying.transition)
        assertFalse(staying.shouldWarn)
        assertTrue(second.shouldWarn)
        assertTrue(third.shouldWarn)
    }

    @Test
    fun aWindowThatIsNotARecognisableActivityStillCountsAsEnteringTheApplication() {
        val pipeline = pipeline()
        val over = snapshot(mapOf(youtube to 30))
        val spent = used(mapOf(youtube to 47))

        pipeline.onWindowStateChanged("com.google.android.youtube", over, spent)
        pipeline.onWindowStateChanged("com.android.launcher", over, spent)
        val returning = pipeline.onWindowStateChanged("com.google.android.youtube", over, spent)

        assertTrue(returning.shouldWarn)
    }

    @Test
    fun ourOwnWarningWindowDoesNotEndTheVisitAndCauseItToReappear() {
        val pipeline = pipeline()
        val over = snapshot(mapOf(youtube to 30))
        val spent = used(mapOf(youtube to 47))

        pipeline.onWindowStateChanged("com.google.android.youtube", over, spent)
        val ours = pipeline.onWindowStateChanged("com.blocksocial.lite", over, spent)
        val back = pipeline.onWindowStateChanged("com.google.android.youtube", over, spent)

        assertEquals(TransitionOutcome.IGNORED_SELF, ours.transition)
        assertEquals(TransitionOutcome.IGNORED_ALREADY_FOREGROUND, back.transition)
    }

    @Test
    fun theStatusBarComingDownDoesNotEndTheVisit() {
        val pipeline = pipeline()
        val over = snapshot(mapOf(youtube to 30))
        val spent = used(mapOf(youtube to 47))

        pipeline.onWindowStateChanged("com.google.android.youtube", over, spent)
        val shade = pipeline.onWindowStateChanged("com.android.systemui", over, spent)
        val back = pipeline.onWindowStateChanged("com.google.android.youtube", over, spent)

        assertEquals(TransitionOutcome.IGNORED_OVERLAY, shade.transition)
        assertEquals(TransitionOutcome.IGNORED_ALREADY_FOREGROUND, back.transition)
    }

    @Test
    fun theMeasurementIsReadOnlyWhenAVisitToALimitedApplicationBegins() {
        var reads = 0
        val readUsage = { reads++; UsageToday.Unavailable }
        val pipeline = pipeline()
        val limited = snapshot(mapOf(youtube to 30))

        pipeline.onWindowStateChanged("com.android.settings", limited, readUsage)
        pipeline.onWindowStateChanged("com.instagram.android", limited, readUsage)
        assertEquals(0, reads)

        pipeline.onWindowStateChanged("com.google.android.youtube", limited, readUsage)
        pipeline.onWindowStateChanged("com.google.android.youtube", limited, readUsage)
        assertEquals(1, reads)
    }

    @Test
    fun blockSocialLiteNeverWarnsAboutItself() {
        val result = pipeline().onWindowStateChanged(
            "com.blocksocial.lite",
            snapshot(mapOf(youtube to 30)),
            used(mapOf(youtube to 47)),
        )

        assertEquals(TransitionOutcome.IGNORED_SELF, result.transition)
    }

    @Test
    fun aCriticalSystemApplicationIsNeverWarnedAbout() {
        val result = pipeline().onWindowStateChanged(
            "com.android.settings",
            snapshot(mapOf(youtube to 30)),
            used(mapOf(youtube to 47)),
        )

        assertEquals(TransitionOutcome.IGNORED_SYSTEM, result.transition)
    }
}
