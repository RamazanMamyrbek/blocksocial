package com.blocksocial.spike.a01

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchDetectorTest {

    private val instagram = "com.instagram.android"
    private val tiktok = "com.zhiliaoapp.musically"
    private val self = "com.blocksocial.spike.a01"
    private val launcher = "com.android.launcher3"
    private val settings = "com.android.settings"
    private val unlisted = "com.example.notes"

    private fun detector(debounceWindowMillis: Long = 1_000L) = LaunchDetector(
        targetPackages = setOf(instagram, tiktok),
        selfPackage = self,
        systemPackages = setOf(launcher, settings),
        debounceWindowMillis = debounceWindowMillis
    )

    @Test
    fun `entering a target application is detected`() {
        assertEquals(
            TransitionDecision.TARGET_ENTERED,
            detector().onForegroundPackageChanged(instagram, 1_000L)
        )
    }

    @Test
    fun `repeated events for the same target inside the window are debounced`() {
        val detector = detector()
        detector.onForegroundPackageChanged(instagram, 1_000L)

        assertEquals(
            TransitionDecision.IGNORED_DEBOUNCED,
            detector.onForegroundPackageChanged(instagram, 1_100L)
        )
        assertEquals(
            TransitionDecision.IGNORED_DEBOUNCED,
            detector.onForegroundPackageChanged(instagram, 1_999L)
        )
    }

    @Test
    fun `the same target is detected again once the window has passed`() {
        val detector = detector()
        detector.onForegroundPackageChanged(instagram, 1_000L)

        assertEquals(
            TransitionDecision.TARGET_ENTERED,
            detector.onForegroundPackageChanged(instagram, 2_000L)
        )
    }

    @Test
    fun `leaving to a non-target allows immediate re-entry to be detected`() {
        val detector = detector()
        detector.onForegroundPackageChanged(instagram, 1_000L)

        assertEquals(
            TransitionDecision.IGNORED_SYSTEM,
            detector.onForegroundPackageChanged(launcher, 1_100L)
        )
        assertEquals(
            TransitionDecision.TARGET_ENTERED,
            detector.onForegroundPackageChanged(instagram, 1_200L)
        )
    }

    @Test
    fun `switching between two targets is detected each time`() {
        val detector = detector()

        assertEquals(
            TransitionDecision.TARGET_ENTERED,
            detector.onForegroundPackageChanged(instagram, 1_000L)
        )
        assertEquals(
            TransitionDecision.TARGET_ENTERED,
            detector.onForegroundPackageChanged(tiktok, 1_100L)
        )
        assertEquals(
            TransitionDecision.TARGET_ENTERED,
            detector.onForegroundPackageChanged(instagram, 1_200L)
        )
    }

    @Test
    fun `own package is ignored and does not reset the debounce window`() {
        val detector = detector()
        detector.onForegroundPackageChanged(instagram, 1_000L)

        assertEquals(
            TransitionDecision.IGNORED_SELF,
            detector.onForegroundPackageChanged(self, 1_100L)
        )
        assertEquals(
            TransitionDecision.IGNORED_DEBOUNCED,
            detector.onForegroundPackageChanged(instagram, 1_200L)
        )
    }

    @Test
    fun `system packages are ignored`() {
        val detector = detector()

        assertEquals(
            TransitionDecision.IGNORED_SYSTEM,
            detector.onForegroundPackageChanged(launcher, 1_000L)
        )
        assertEquals(
            TransitionDecision.IGNORED_SYSTEM,
            detector.onForegroundPackageChanged(settings, 1_100L)
        )
    }

    @Test
    fun `packages outside the target set are ignored`() {
        assertEquals(
            TransitionDecision.IGNORED_NOT_TARGET,
            detector().onForegroundPackageChanged(unlisted, 1_000L)
        )
    }

    @Test
    fun `missing package names are ignored`() {
        val detector = detector()

        assertEquals(
            TransitionDecision.IGNORED_UNKNOWN_PACKAGE,
            detector.onForegroundPackageChanged(null, 1_000L)
        )
        assertEquals(
            TransitionDecision.IGNORED_UNKNOWN_PACKAGE,
            detector.onForegroundPackageChanged("", 1_100L)
        )
    }

    @Test
    fun `an event flood on one target produces a single detection`() {
        val detector = detector()
        val decisions = (0 until 50).map { index ->
            detector.onForegroundPackageChanged(instagram, 1_000L + index * 10L)
        }

        assertEquals(1, decisions.count { it == TransitionDecision.TARGET_ENTERED })
        assertEquals(49, decisions.count { it == TransitionDecision.IGNORED_DEBOUNCED })
    }
}
