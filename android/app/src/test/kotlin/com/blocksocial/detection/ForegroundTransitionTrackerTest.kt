package com.blocksocial.detection

import org.junit.Assert.assertEquals
import org.junit.Test

class ForegroundTransitionTrackerTest {

    private val self = "com.blocksocial"
    private val launcher = "com.android.launcher"
    private val target = "com.instagram.android"

    private val systemUi = "com.android.systemui"

    private fun tracker() = ForegroundTransitionTracker(
        selfPackage = self,
        systemPackages = setOf(launcher, systemUi, "com.android.settings"),
        overlayPackages = setOf(systemUi),
    )

    private fun ForegroundTransitionTracker.enter(
        packageName: String?,
        isTarget: Boolean = true,
        isActivity: Boolean = true,
    ) = onWindowStateChanged(packageName, isTarget) { isActivity }

    @Test
    fun enteringATargetApplicationIsDetected() {
        assertEquals(TransitionOutcome.TARGET_ENTERED, tracker().enter(target))
    }

    @Test
    fun aColdLaunchThatEmitsSeveralActivityWindowsIsOneVisit() {
        val tracker = tracker()

        val outcomes = listOf(
            tracker.enter(target),
            tracker.enter(target),
            tracker.enter(target),
        )

        assertEquals(
            listOf(
                TransitionOutcome.TARGET_ENTERED,
                TransitionOutcome.IGNORED_ALREADY_FOREGROUND,
                TransitionOutcome.IGNORED_ALREADY_FOREGROUND,
            ),
            outcomes,
        )
    }

    @Test
    fun goingHomeAndReturningIsDetectedAgain() {
        val tracker = tracker()
        tracker.enter(target)

        tracker.enter(launcher, isTarget = false)

        assertEquals(TransitionOutcome.TARGET_ENTERED, tracker.enter(target))
    }

    @Test
    fun leavingToAnotherApplicationEndsTheVisit() {
        val tracker = tracker()
        tracker.enter(target)

        tracker.enter("com.example.notes", isTarget = false)

        assertEquals(TransitionOutcome.TARGET_ENTERED, tracker.enter(target))
    }

    @Test
    fun aWindowThatIsNotAnActivityIsNeverAnEntry() {
        assertEquals(
            TransitionOutcome.IGNORED_NOT_AN_ACTIVITY,
            tracker().enter(target, isActivity = false),
        )
    }

    @Test
    fun aBackgroundingApplicationCannotReEnterThroughANonActivityWindow() {
        val tracker = tracker()
        tracker.enter(target)
        tracker.enter(launcher, isTarget = false)

        val outcome = tracker.enter(target, isActivity = false)

        assertEquals(TransitionOutcome.IGNORED_NOT_AN_ACTIVITY, outcome)
    }

    @Test
    fun ourOwnWindowDoesNotEndTheVisit() {
        val tracker = tracker()
        tracker.enter(target)

        assertEquals(TransitionOutcome.IGNORED_SELF, tracker.enter(self, isTarget = false))
        assertEquals(TransitionOutcome.IGNORED_ALREADY_FOREGROUND, tracker.enter(target))
    }

    @Test
    fun systemPackagesAreNeverTargets() {
        val tracker = tracker()

        assertEquals(TransitionOutcome.IGNORED_SYSTEM, tracker.enter("com.android.settings"))
        assertEquals(TransitionOutcome.IGNORED_OVERLAY, tracker.enter(systemUi))
        assertEquals(TransitionOutcome.IGNORED_SYSTEM, tracker.enter(launcher))
    }

    @Test
    fun theNotificationShadeDoesNotEndTheVisit() {
        val tracker = tracker()
        tracker.enter(target)

        assertEquals(TransitionOutcome.IGNORED_OVERLAY, tracker.enter(systemUi, isTarget = false))

        assertEquals(TransitionOutcome.IGNORED_ALREADY_FOREGROUND, tracker.enter(target))
    }

    @Test
    fun theScreenGoingOffEndsTheVisitSoUnlockingIsANewEntry() {
        val tracker = tracker()
        tracker.enter(target)

        tracker.forget()

        assertEquals(TransitionOutcome.TARGET_ENTERED, tracker.enter(target))
    }

    @Test
    fun anApplicationOutsideTheSelectionIsIgnored() {
        assertEquals(
            TransitionOutcome.IGNORED_NOT_A_TARGET,
            tracker().enter("com.example.notes", isTarget = false),
        )
    }

    @Test
    fun anEmptyPackageNameIsIgnored() {
        assertEquals(TransitionOutcome.IGNORED_UNKNOWN_PACKAGE, tracker().enter(null))
        assertEquals(TransitionOutcome.IGNORED_UNKNOWN_PACKAGE, tracker().enter(""))
    }

    @Test
    fun theActivityCheckIsOnlyPaidForTargetApplications() {
        var activityChecks = 0
        val tracker = tracker()

        tracker.onWindowStateChanged("com.android.settings", isTargetPackage = false) {
            activityChecks++
            true
        }
        tracker.onWindowStateChanged("com.example.notes", isTargetPackage = false) {
            activityChecks++
            true
        }

        assertEquals(0, activityChecks)
    }

    @Test
    fun forgettingTheForegroundMakesTheNextWindowAnEntryAgain() {
        val tracker = tracker()
        tracker.enter(target)

        tracker.forget()

        assertEquals(TransitionOutcome.TARGET_ENTERED, tracker.enter(target))
    }
}
