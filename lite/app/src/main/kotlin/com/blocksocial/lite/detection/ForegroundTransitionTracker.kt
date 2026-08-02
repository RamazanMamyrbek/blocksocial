package com.blocksocial.lite.detection

enum class TransitionOutcome {
    IGNORED_UNKNOWN_PACKAGE,
    IGNORED_SELF,
    IGNORED_OVERLAY,
    IGNORED_SYSTEM,
    IGNORED_NOT_A_TARGET,
    IGNORED_NOT_AN_ACTIVITY,
    IGNORED_ALREADY_FOREGROUND,
    TARGET_ENTERED,
}

class ForegroundTransitionTracker(
    private val selfPackage: String,
    private val systemPackages: Set<String>,
    private val overlayPackages: Set<String>,
) {

    private var currentForegroundPackage: String? = null

    fun onWindowStateChanged(
        packageName: String?,
        isTargetPackage: Boolean,
        isActivityWindow: () -> Boolean,
    ): TransitionOutcome {
        if (packageName.isNullOrBlank()) return TransitionOutcome.IGNORED_UNKNOWN_PACKAGE
        if (packageName == selfPackage) return TransitionOutcome.IGNORED_SELF
        if (packageName in overlayPackages) return TransitionOutcome.IGNORED_OVERLAY

        if (packageName in systemPackages) {
            currentForegroundPackage = null
            return TransitionOutcome.IGNORED_SYSTEM
        }
        if (!isTargetPackage) {
            currentForegroundPackage = null
            return TransitionOutcome.IGNORED_NOT_A_TARGET
        }
        if (!isActivityWindow()) return TransitionOutcome.IGNORED_NOT_AN_ACTIVITY
        if (packageName == currentForegroundPackage) return TransitionOutcome.IGNORED_ALREADY_FOREGROUND

        currentForegroundPackage = packageName
        return TransitionOutcome.TARGET_ENTERED
    }

    fun forget() {
        currentForegroundPackage = null
    }
}
