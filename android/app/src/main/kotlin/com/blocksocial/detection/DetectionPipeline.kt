package com.blocksocial.detection

import com.blocksocial.core.domain.RestrictionDecision
import com.blocksocial.core.domain.RuleEvaluator
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.DeviceTime

data class DetectionResult(
    val transition: TransitionOutcome,
    val app: AppRef?,
    val decision: RestrictionDecision?,
) {
    val shouldBlock: Boolean = decision?.restrictionActive == true
}

class DetectionPipeline(
    selfPackage: String,
    systemPackages: Set<String>,
    overlayPackages: Set<String> = SystemPackages.NEVER_TAKES_THE_FOREGROUND,
    private val isActivityWindow: (String, String?) -> Boolean,
    private val readDeviceTime: () -> DeviceTime,
) {

    private val tracker = ForegroundTransitionTracker(selfPackage, systemPackages, overlayPackages)

    fun onWindowStateChanged(
        packageName: String?,
        className: String?,
        snapshot: ProtectionSnapshot,
    ): DetectionResult {
        val app = packageName?.let(snapshot.packageToApp::get)
        val transition = tracker.onWindowStateChanged(
            packageName = packageName,
            isTargetPackage = app != null,
            isActivityWindow = { isActivityWindow(packageName.orEmpty(), className) },
        )

        if (transition != TransitionOutcome.TARGET_ENTERED || app == null) {
            return DetectionResult(transition, app, decision = null)
        }

        val decision = RuleEvaluator.evaluate(
            rules = snapshot.rulesByApp[app].orEmpty(),
            grant = snapshot.grantsByApp[app],
            usageSessions = emptyList(),
            forApp = app,
            at = readDeviceTime(),
            usageMeasurementAvailable = USAGE_MEASUREMENT_ARRIVES_IN_PHASE_SEVENTEEN,
        )
        return DetectionResult(transition, app, decision)
    }

    fun forgetForeground() = tracker.forget()

    private companion object {
        const val USAGE_MEASUREMENT_ARRIVES_IN_PHASE_SEVENTEEN = false
    }
}
