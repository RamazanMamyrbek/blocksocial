package com.blocksocial.lite.detection

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.blocksocial.lite.container
import com.blocksocial.lite.ui.LiteTheme
import com.blocksocial.lite.usage.UsageToday
import com.blocksocial.lite.warning.LimitReachedScreen
import com.blocksocial.lite.warning.WarningOverlayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LimitAccessibilityService : AccessibilityService() {

    @Volatile
    private var snapshot: LimitSnapshot = LimitSnapshot.Empty

    @Volatile
    private var usage: UsageToday = UsageToday.Unavailable

    private var pipeline: DetectionPipeline? = null
    private var overlay: WarningOverlayController? = null
    private var scope: CoroutineScope? = null

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            pipeline?.forgetForeground()
            overlay?.dismiss()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        val catalog = container.catalog
        pipeline = DetectionPipeline(
            selfPackage = packageName,
            systemPackages = SystemPackages.resolve(this),
            isActivityWindow = ::isActivityWindow,
        )
        overlay = WarningOverlayController(
            context = this,
            windowManager = getSystemService(WindowManager::class.java),
            onDismissed = {},
        )
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

        val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope = serviceScope
        serviceScope.launch {
            container.limits.limits.collect { limits ->
                snapshot = LimitSnapshot(
                    packageToApp = catalog.packageToApp(),
                    displayNames = catalog.displayNames(),
                    limits = limits,
                )
                refreshUsage()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val activePipeline = pipeline ?: return

        val result = activePipeline.onWindowStateChanged(
            packageName = event.packageName?.toString(),
            className = event.className?.toString(),
            snapshot = snapshot,
            readUsage = ::measurementStillPermitted,
        )

        if (result.transition.movesTheForeground()) overlay?.dismissIfForegroundLeft(result.app)
        if (result.transition.endsAVisit()) refreshUsage()

        val status = result.status
        if (result.shouldWarn && result.app != null && status != null) {
            val app = result.app
            val name = snapshot.displayNames[app] ?: app
            overlay?.show(app) {
                LiteTheme {
                    LimitReachedScreen(
                        appDisplayName = name,
                        usedMinutes = status.usedMinutes ?: 0,
                        limitMinutes = status.limitMinutes,
                        onLeave = {
                            overlay?.dismiss()
                            performGlobalAction(GLOBAL_ACTION_HOME)
                        },
                        onContinue = { overlay?.dismiss() },
                    )
                }
            }
        }
    }

    private fun measurementStillPermitted(): UsageToday =
        if (container.usage.hasUsageAccess()) usage else UsageToday.Unavailable

    private fun refreshUsage() {
        val current = snapshot
        scope?.launch {
            usage = runCatching { container.usage.readToday(current.packageToApp) }
                .getOrDefault(UsageToday.Unavailable)
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        overlay?.dismiss()
        runCatching { unregisterReceiver(screenOffReceiver) }
        scope?.cancel()
        scope = null
        pipeline = null
        overlay = null
        snapshot = LimitSnapshot.Empty
        return super.onUnbind(intent)
    }

    private fun isActivityWindow(packageName: String, className: String?): Boolean {
        if (packageName.isBlank() || className.isNullOrBlank()) return false
        return try {
            packageManager.getActivityInfo(ComponentName(packageName, className), 0)
            true
        } catch (notAnActivity: PackageManager.NameNotFoundException) {
            false
        }
    }
}

private fun TransitionOutcome.endsAVisit(): Boolean = when (this) {
    TransitionOutcome.IGNORED_SYSTEM,
    TransitionOutcome.IGNORED_NOT_A_TARGET,
    -> true

    TransitionOutcome.TARGET_ENTERED,
    TransitionOutcome.IGNORED_UNKNOWN_PACKAGE,
    TransitionOutcome.IGNORED_SELF,
    TransitionOutcome.IGNORED_OVERLAY,
    TransitionOutcome.IGNORED_NOT_AN_ACTIVITY,
    TransitionOutcome.IGNORED_ALREADY_FOREGROUND,
    -> false
}

private fun TransitionOutcome.movesTheForeground(): Boolean = when (this) {
    TransitionOutcome.TARGET_ENTERED,
    TransitionOutcome.IGNORED_SYSTEM,
    TransitionOutcome.IGNORED_NOT_A_TARGET,
    -> true

    TransitionOutcome.IGNORED_UNKNOWN_PACKAGE,
    TransitionOutcome.IGNORED_SELF,
    TransitionOutcome.IGNORED_OVERLAY,
    TransitionOutcome.IGNORED_NOT_AN_ACTIVITY,
    TransitionOutcome.IGNORED_ALREADY_FOREGROUND,
    -> false
}
