package com.blocksocial.spike.a01

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchDetectorTest {

    private val youtube = "com.google.android.youtube"
    private val chrome = "com.android.chrome"
    private val self = "com.blocksocial.spike.a01"
    private val launcher = "com.google.android.apps.nexuslauncher"
    private val settings = "com.android.settings"
    private val unlisted = "com.google.android.contacts"

    private fun detector(debounceWindowMillis: Long = 1_000L) = LaunchDetector(
        targetPackages = setOf(youtube, chrome),
        selfPackage = self,
        systemPackages = setOf(launcher, settings),
        debounceWindowMillis = debounceWindowMillis
    )

    private fun LaunchDetector.activityWindow(packageName: String?, atMillis: Long) =
        onForegroundPackageChanged(packageName, isActivityWindow = true, atMillis = atMillis)

    private fun LaunchDetector.otherWindow(packageName: String?, atMillis: Long) =
        onForegroundPackageChanged(packageName, isActivityWindow = false, atMillis = atMillis)

    @Test
    fun `entering a target application is detected`() {
        assertEquals(
            TransitionDecision.TARGET_ENTERED,
            detector().activityWindow(youtube, 1_000L)
        )
    }

    @Test
    fun `a non-activity window of a target is not an entry`() {
        assertEquals(
            TransitionDecision.IGNORED_NOT_AN_ACTIVITY,
            detector().otherWindow(youtube, 1_000L)
        )
    }

    @Test
    fun `a non-activity window while backgrounding does not start a visit`() {
        val detector = detector()

        assertEquals(
            TransitionDecision.TARGET_ENTERED,
            detector.activityWindow(youtube, 1_000L)
        )
        assertEquals(
            TransitionDecision.IGNORED_SYSTEM,
            detector.activityWindow(launcher, 5_000L)
        )
        assertEquals(
            TransitionDecision.IGNORED_NOT_AN_ACTIVITY,
            detector.otherWindow(youtube, 6_400L)
        )
        assertEquals(
            TransitionDecision.TARGET_ENTERED,
            detector.activityWindow(youtube, 12_000L)
        )
    }

    @Test
    fun `further windows inside the same target are one visit`() {
        val detector = detector()
        detector.activityWindow(youtube, 1_000L)

        assertEquals(
            TransitionDecision.IGNORED_ALREADY_FOREGROUND,
            detector.activityWindow(youtube, 1_100L)
        )
    }

    @Test
    fun `a splash screen and the main activity are one visit even seconds apart`() {
        val detector = detector()

        assertEquals(
            TransitionDecision.TARGET_ENTERED,
            detector.activityWindow(youtube, 1_000L)
        )
        assertEquals(
            TransitionDecision.IGNORED_ALREADY_FOREGROUND,
            detector.activityWindow(youtube, 4_073L)
        )
        assertEquals(
            TransitionDecision.IGNORED_ALREADY_FOREGROUND,
            detector.activityWindow(youtube, 9_000L)
        )
    }

    @Test
    fun `leaving to a non-target allows immediate re-entry to be detected`() {
        val detector = detector()
        detector.activityWindow(youtube, 1_000L)

        assertEquals(
            TransitionDecision.IGNORED_SYSTEM,
            detector.activityWindow(launcher, 1_100L)
        )
        assertEquals(
            TransitionDecision.TARGET_ENTERED,
            detector.activityWindow(youtube, 1_200L)
        )
    }

    @Test
    fun `switching between two targets is detected each time`() {
        val detector = detector()

        assertEquals(
            TransitionDecision.TARGET_ENTERED,
            detector.activityWindow(youtube, 1_000L)
        )
        assertEquals(
            TransitionDecision.TARGET_ENTERED,
            detector.activityWindow(chrome, 4_000L)
        )
        assertEquals(
            TransitionDecision.TARGET_ENTERED,
            detector.activityWindow(youtube, 7_000L)
        )
    }

    @Test
    fun `own package does not end the current visit`() {
        val detector = detector()
        detector.activityWindow(youtube, 1_000L)

        assertEquals(
            TransitionDecision.IGNORED_SELF,
            detector.activityWindow(self, 1_100L)
        )
        assertEquals(
            TransitionDecision.IGNORED_ALREADY_FOREGROUND,
            detector.activityWindow(youtube, 1_200L)
        )
    }

    @Test
    fun `system packages are ignored`() {
        val detector = detector()

        assertEquals(
            TransitionDecision.IGNORED_SYSTEM,
            detector.activityWindow(launcher, 1_000L)
        )
        assertEquals(
            TransitionDecision.IGNORED_SYSTEM,
            detector.activityWindow(settings, 1_100L)
        )
    }

    @Test
    fun `packages outside the target set are ignored`() {
        assertEquals(
            TransitionDecision.IGNORED_NOT_TARGET,
            detector().activityWindow(unlisted, 1_000L)
        )
    }

    @Test
    fun `missing package names are ignored`() {
        val detector = detector()

        assertEquals(
            TransitionDecision.IGNORED_UNKNOWN_PACKAGE,
            detector.activityWindow(null, 1_000L)
        )
        assertEquals(
            TransitionDecision.IGNORED_UNKNOWN_PACKAGE,
            detector.activityWindow("", 1_100L)
        )
    }

    @Test
    fun `an event flood on one target produces a single detection`() {
        val detector = detector()
        val decisions = (0 until 50).map { index ->
            detector.activityWindow(youtube, 1_000L + index * 200L)
        }

        assertEquals(1, decisions.count { it == TransitionDecision.TARGET_ENTERED })
        assertEquals(49, decisions.count { it == TransitionDecision.IGNORED_ALREADY_FOREGROUND })
    }

    @Test
    fun `two separate visits to the same target are two detections`() {
        val detector = detector()
        val decisions = listOf(
            detector.activityWindow(youtube, 1_000L),
            detector.activityWindow(launcher, 5_000L),
            detector.activityWindow(youtube, 9_000L)
        )

        assertEquals(
            listOf(
                TransitionDecision.TARGET_ENTERED,
                TransitionDecision.IGNORED_SYSTEM,
                TransitionDecision.TARGET_ENTERED
            ),
            decisions
        )
    }
}
