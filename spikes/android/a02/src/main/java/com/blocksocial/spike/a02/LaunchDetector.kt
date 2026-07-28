package com.blocksocial.spike.a02

enum class TransitionDecision {
    TARGET_ENTERED,
    IGNORED_UNKNOWN_PACKAGE,
    IGNORED_SELF,
    IGNORED_NOT_AN_ACTIVITY,
    IGNORED_SYSTEM,
    IGNORED_NOT_TARGET,
    IGNORED_ALREADY_FOREGROUND,
    IGNORED_DEBOUNCED
}

class LaunchDetector(
    private val targetPackages: Set<String>,
    private val selfPackage: String,
    private val systemPackages: Set<String>,
    private val debounceWindowMillis: Long
) {

    private var currentForegroundPackage: String? = null
    private var lastHandledPackage: String? = null
    private var lastHandledAt: Long = 0L

    fun onForegroundPackageChanged(
        packageName: String?,
        isActivityWindow: Boolean,
        atMillis: Long
    ): TransitionDecision {
        if (packageName.isNullOrBlank()) {
            return TransitionDecision.IGNORED_UNKNOWN_PACKAGE
        }
        if (packageName == selfPackage) {
            return TransitionDecision.IGNORED_SELF
        }
        if (packageName in systemPackages) {
            leaveForeground(packageName)
            return TransitionDecision.IGNORED_SYSTEM
        }
        if (packageName !in targetPackages) {
            leaveForeground(packageName)
            return TransitionDecision.IGNORED_NOT_TARGET
        }
        if (!isActivityWindow) {
            return TransitionDecision.IGNORED_NOT_AN_ACTIVITY
        }
        if (packageName == currentForegroundPackage) {
            return TransitionDecision.IGNORED_ALREADY_FOREGROUND
        }
        currentForegroundPackage = packageName
        if (packageName == lastHandledPackage && atMillis - lastHandledAt < debounceWindowMillis) {
            return TransitionDecision.IGNORED_DEBOUNCED
        }
        lastHandledPackage = packageName
        lastHandledAt = atMillis
        return TransitionDecision.TARGET_ENTERED
    }

    private fun leaveForeground(packageName: String) {
        currentForegroundPackage = packageName
        lastHandledPackage = null
        lastHandledAt = 0L
    }
}
