package com.blocksocial.lite.detection

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val MEASUREMENT_INTERVAL_MILLIS = 30_000L

class LimitAccessibilityService : AccessibilityService() {

    @Volatile
    private var snapshot: LimitSnapshot = LimitSnapshot.Empty

    @Volatile
    private var usage: UsageToday = UsageToday.Unavailable

    @Volatile
    private var appInFront: String? = null

    private var pipeline: DetectionPipeline? = null
    private var overlay: WarningOverlayController? = null
    private var scope: CoroutineScope? = null
    private val main = Handler(Looper.getMainLooper())

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            pipeline?.forgetForeground()
            appInFront = null
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

        snapshot = container.lastKnownSnapshot
        usage = container.lastKnownUsage

        val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = serviceScope
        serviceScope.launch {
            container.limits.limits.collect { limits ->
                val fresh = LimitSnapshot(
                    packageToApp = catalog.packageToApp(),
                    displayNames = catalog.displayNames(),
                    limits = limits,
                )
                snapshot = fresh
                container.lastKnownSnapshot = fresh
                if (fresh.limits.isEmpty()) {
                    LimitGuardService.stop(this@LimitAccessibilityService)
                } else {
                    LimitGuardService.keepRunning(this@LimitAccessibilityService)
                }
            }
        }
        serviceScope.launch {
            while (isActive) {
                measure()
                delay(MEASUREMENT_INTERVAL_MILLIS)
            }
        }

        container.heartbeat.onConnected(this, System.currentTimeMillis())
        LimitGuardService.keepRunning(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        container.heartbeat.onEvent(System.currentTimeMillis())
        val activePipeline = pipeline ?: return

        val result = activePipeline.onWindowStateChanged(
            packageName = event.packageName?.toString(),
            snapshot = snapshot,
            readUsage = ::permittedUsage,
        )

        if (result.transition.movesTheForeground()) {
            appInFront = result.app
            overlay?.dismissIfForegroundLeft(result.app)
        }

        container.decisions.record(
            DecisionRecord(
                atMillis = System.currentTimeMillis(),
                app = result.app,
                transition = result.transition,
                usedMinutes = result.status?.usedMinutes,
                limitMinutes = result.status?.limitMinutes,
                warned = result.shouldWarn,
            ),
        )

        if (result.transition != TransitionOutcome.TARGET_ENTERED) return

        val status = result.status
        if (result.shouldWarn && result.app != null && status != null) {
            warnAbout(result.app, status.usedMinutes ?: 0, status.limitMinutes)
        } else {
            scope?.launch { measure(); reconsider() }
        }
    }

    private fun measure() {
        val current = snapshot
        val reading = runCatching { container.usage.readToday(current.packageToApp) }
            .getOrDefault(UsageToday.Unavailable)
        usage = reading
        container.lastKnownUsage = reading
    }

    private fun reconsider() {
        val app = appInFront ?: return
        val limit = snapshot.limits[app] ?: return
        val used = permittedUsage().minutesFor(app) ?: return
        if (used < limit) return
        container.decisions.record(
            DecisionRecord(
                atMillis = System.currentTimeMillis(),
                app = app,
                transition = TransitionOutcome.TARGET_ENTERED,
                usedMinutes = used,
                limitMinutes = limit,
                warned = true,
            ),
        )
        warnAbout(app, used, limit)
    }

    private fun warnAbout(app: String, usedMinutes: Int, limitMinutes: Int) {
        val name = snapshot.displayNames[app] ?: app
        main.post {
            if (appInFront != app) return@post
            overlay?.show(app) {
                LiteTheme {
                    LimitReachedScreen(
                        appDisplayName = name,
                        usedMinutes = usedMinutes,
                        limitMinutes = limitMinutes,
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

    private fun permittedUsage(): UsageToday =
        if (container.usage.hasUsageAccess()) usage else UsageToday.Unavailable

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        overlay?.dismiss()
        runCatching { unregisterReceiver(screenOffReceiver) }
        scope?.cancel()
        scope = null
        pipeline = null
        overlay = null
        appInFront = null
        container.heartbeat.onDisconnected(this)
        LimitGuardService.stop(this)
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
