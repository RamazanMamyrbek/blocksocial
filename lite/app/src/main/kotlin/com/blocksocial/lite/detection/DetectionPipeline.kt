package com.blocksocial.lite.detection

import com.blocksocial.lite.domain.DailyLimit
import com.blocksocial.lite.domain.LimitStatus
import com.blocksocial.lite.usage.UsageToday

data class LimitSnapshot(
    val packageToApp: Map<String, String> = emptyMap(),
    val displayNames: Map<String, String> = emptyMap(),
    val limits: Map<String, Int> = emptyMap(),
) {
    companion object {
        val Empty = LimitSnapshot()
    }
}

data class DetectionResult(
    val transition: TransitionOutcome,
    val app: String?,
    val status: LimitStatus?,
) {
    val shouldWarn: Boolean = status?.reached == true
}

class DetectionPipeline(
    selfPackage: String,
    systemPackages: Set<String>,
    overlayPackages: Set<String> = SystemPackages.NEVER_TAKES_THE_FOREGROUND,
) {

    private val tracker = ForegroundTransitionTracker(selfPackage, systemPackages, overlayPackages)

    fun onWindowStateChanged(
        packageName: String?,
        snapshot: LimitSnapshot,
        readUsage: () -> UsageToday = { UsageToday.Unavailable },
    ): DetectionResult {
        val app = packageName?.let(snapshot.packageToApp::get)?.takeIf { it in snapshot.limits }
        val transition = tracker.onWindowStateChanged(packageName, isTargetPackage = app != null)

        if (transition != TransitionOutcome.TARGET_ENTERED || app == null) {
            return DetectionResult(transition, app, status = null)
        }

        val usage = readUsage()
        return DetectionResult(
            transition = transition,
            app = app,
            status = DailyLimit.statusOf(snapshot.limits[app], usage.minutesFor(app)),
        )
    }

    fun forgetForeground() = tracker.forget()
}
