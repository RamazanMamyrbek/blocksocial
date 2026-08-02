package com.blocksocial.lite.detection

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
        )
        overlay = WarningOverlayController(
            context = this,
            windowManager = getSystemService(WindowManager::class.java),
            onFailure = container.heartbeat::onOverlayFailed,
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
            }
        }

        container.heartbeat.onConnected(this, System.currentTimeMillis())
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        container.heartbeat.onEvent(System.currentTimeMillis())
        val activePipeline = pipeline ?: return

        val result = activePipeline.onWindowStateChanged(
            packageName = event.packageName?.toString(),
            snapshot = snapshot,
            readUsage = ::measureNow,
        )

        if (result.transition.movesTheForeground()) overlay?.dismissIfForegroundLeft(result.app)

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

    private fun measureNow(): UsageToday {
        val current = snapshot
        return runCatching {
            container.usage.readToday(
                packageToApp = current.packageToApp,
                countFromByApp = current.limits.mapValues { (_, limit) -> limit.countingFromMillis },
            )
        }.getOrDefault(UsageToday.Unavailable)
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
        container.heartbeat.onDisconnected(this)
        return super.onUnbind(intent)
    }
}

private fun TransitionOutcome.movesTheForeground(): Boolean = when (this) {
    TransitionOutcome.TARGET_ENTERED,
    TransitionOutcome.IGNORED_SYSTEM,
    TransitionOutcome.IGNORED_NOT_A_TARGET,
    -> true

    TransitionOutcome.IGNORED_UNKNOWN_PACKAGE,
    TransitionOutcome.IGNORED_SELF,
    TransitionOutcome.IGNORED_OVERLAY,
    TransitionOutcome.IGNORED_ALREADY_FOREGROUND,
    -> false
}
