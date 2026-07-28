package com.blocksocial.spike.a02

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchDetectorTest {

    private val youtube = "com.google.android.youtube"
    private val chrome = "com.android.chrome"
    private val self = "com.blocksocial.spike.a02"
    private val launcher = "com.google.android.apps.nexuslauncher"

    private fun detector() = LaunchDetector(
        targetPackages = setOf(youtube, chrome),
        selfPackage = self,
        systemPackages = setOf(launcher),
        debounceWindowMillis = 1_000L
    )

    private fun LaunchDetector.activityWindow(packageName: String?, atMillis: Long) =
        onForegroundPackageChanged(packageName, isActivityWindow = true, atMillis = atMillis)

    private fun LaunchDetector.otherWindow(packageName: String?, atMillis: Long) =
        onForegroundPackageChanged(packageName, isActivityWindow = false, atMillis = atMillis)

    @Test
    fun `one launch raises one block`() {
        val detector = detector()
        val decisions = listOf(
            detector.otherWindow(youtube, 1_000L),
            detector.activityWindow(youtube, 2_000L),
            detector.activityWindow(youtube, 5_000L)
        )

        assertEquals(1, decisions.count { it == TransitionDecision.TARGET_ENTERED })
    }

    @Test
    fun `the overlay is dismissed when the launcher takes over`() {
        val detector = detector()
        detector.activityWindow(youtube, 1_000L)

        assertEquals(
            TransitionDecision.IGNORED_SYSTEM,
            detector.activityWindow(launcher, 2_000L)
        )
    }

    @Test
    fun `returning after the launcher raises a new block`() {
        val detector = detector()
        detector.activityWindow(youtube, 1_000L)
        detector.activityWindow(launcher, 2_000L)

        assertEquals(
            TransitionDecision.TARGET_ENTERED,
            detector.activityWindow(youtube, 3_000L)
        )
    }

    @Test
    fun `the overlay package does not raise a block`() {
        assertEquals(
            TransitionDecision.IGNORED_SELF,
            detector().activityWindow(self, 1_000L)
        )
    }

    @Test
    fun `switching between two targets raises a block for each`() {
        val detector = detector()
        val decisions = listOf(
            detector.activityWindow(youtube, 1_000L),
            detector.activityWindow(chrome, 4_000L)
        )

        assertEquals(2, decisions.count { it == TransitionDecision.TARGET_ENTERED })
    }
}
