package com.blocksocial.spike.a01

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class LaunchDetectionService : AccessibilityService() {

    private var detector: LaunchDetector? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        detector = LaunchDetector(
            targetPackages = TARGET_PACKAGES,
            selfPackage = packageName,
            systemPackages = SystemPackages.resolve(this),
            debounceWindowMillis = DEBOUNCE_WINDOW_MILLIS
        )
        log("connected targets=$TARGET_PACKAGES debounceMillis=$DEBOUNCE_WINDOW_MILLIS")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }
        val activeDetector = detector ?: return
        val foregroundPackage = event.packageName?.toString()
        val eventTime = event.eventTime
        val isActivityWindow = isActivityWindow(foregroundPackage, event.className?.toString())
        val decision = activeDetector.onForegroundPackageChanged(
            foregroundPackage,
            isActivityWindow,
            eventTime
        )
        val deliveryLatencyMillis = SystemClock.uptimeMillis() - eventTime
        log("package=$foregroundPackage class=${event.className} activity=$isActivityWindow decision=$decision deliveryLatencyMillis=$deliveryLatencyMillis")
    }

    private fun isActivityWindow(packageName: String?, className: String?): Boolean {
        if (packageName.isNullOrBlank() || className.isNullOrBlank()) {
            return false
        }
        return try {
            packageManager.getActivityInfo(ComponentName(packageName, className), 0)
            true
        } catch (notAnActivity: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        detector = null
        log("unbound")
        return super.onUnbind(intent)
    }

    private fun log(message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, message)
        }
    }

    private companion object {
        const val TAG = "SpikeA01"
        const val DEBOUNCE_WINDOW_MILLIS = 1_000L
        val TARGET_PACKAGES = setOf("com.google.android.youtube", "com.android.chrome")
    }
}
