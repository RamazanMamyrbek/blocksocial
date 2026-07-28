package com.blocksocial.spike.a01

enum class TransitionDecision {
    TARGET_ENTERED,
    IGNORED_UNKNOWN_PACKAGE,
    IGNORED_SELF,
    IGNORED_SYSTEM,
    IGNORED_NOT_TARGET,
    IGNORED_DEBOUNCED
}

class LaunchDetector(
    private val targetPackages: Set<String>,
    private val selfPackage: String,
    private val systemPackages: Set<String>,
    private val debounceWindowMillis: Long
) {

    private var lastHandledPackage: String? = null
    private var lastHandledAt: Long = 0L

    fun onForegroundPackageChanged(packageName: String?, atMillis: Long): TransitionDecision {
        if (packageName.isNullOrBlank()) {
            return TransitionDecision.IGNORED_UNKNOWN_PACKAGE
        }
        if (packageName == selfPackage) {
            return TransitionDecision.IGNORED_SELF
        }
        if (packageName in systemPackages) {
            forgetLastHandled()
            return TransitionDecision.IGNORED_SYSTEM
        }
        if (packageName !in targetPackages) {
            forgetLastHandled()
            return TransitionDecision.IGNORED_NOT_TARGET
        }
        if (packageName == lastHandledPackage && atMillis - lastHandledAt < debounceWindowMillis) {
            return TransitionDecision.IGNORED_DEBOUNCED
        }
        lastHandledPackage = packageName
        lastHandledAt = atMillis
        return TransitionDecision.TARGET_ENTERED
    }

    private fun forgetLastHandled() {
        lastHandledPackage = null
        lastHandledAt = 0L
    }
}
