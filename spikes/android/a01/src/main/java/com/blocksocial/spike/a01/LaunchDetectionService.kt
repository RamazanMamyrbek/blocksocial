package com.blocksocial.spike.a01

import android.accessibilityservice.AccessibilityService
import android.content.Intent
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
        val decision = activeDetector.onForegroundPackageChanged(foregroundPackage, eventTime)
        val deliveryLatencyMillis = SystemClock.uptimeMillis() - eventTime
        log("package=$foregroundPackage decision=$decision deliveryLatencyMillis=$deliveryLatencyMillis")
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
        val TARGET_PACKAGES = setOf("com.instagram.android", "com.zhiliaoapp.musically")
    }
}
