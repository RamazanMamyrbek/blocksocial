package com.blocksocial.detection

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.blocksocial.block.BlockOutcome
import com.blocksocial.block.BlockOverlayController
import com.blocksocial.block.BlockPresentation
import com.blocksocial.block.BlockScreen
import com.blocksocial.core.data.repository.BlockEventRepository
import com.blocksocial.core.domain.RestrictionDecision
import com.blocksocial.core.domain.ScheduleEvaluator
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.BlockEvent
import com.blocksocial.core.model.BypassPolicy
import com.blocksocial.core.model.DeviceTime
import com.blocksocial.core.model.Platform
import com.blocksocial.core.model.RestrictionRule
import com.blocksocial.core.model.RuleMode
import com.blocksocial.core.model.UserAction
import com.blocksocial.core.ui.theme.BlockSocialTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class BlockSocialAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var snapshotSource: ProtectionSnapshotSource

    @Inject
    lateinit var blockEvents: BlockEventRepository

    @Volatile
    private var snapshot: ProtectionSnapshot = ProtectionSnapshot.Empty

    private var pipeline: DetectionPipeline? = null
    private var overlay: BlockOverlayController? = null
    private var scope: CoroutineScope? = null
    private var reasonsWhenShown: List<RuleMode> = listOf(RuleMode.ALWAYS)

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            pipeline?.forgetForeground()
            overlay?.dismiss(BlockOutcome.FOREGROUND_MOVED_ON)
            DetectionLog.lifecycle("screen-off")
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        pipeline = DetectionPipeline(
            selfPackage = packageName,
            systemPackages = SystemPackages.resolve(this),
            isActivityWindow = ::isActivityWindow,
            readDeviceTime = ::readDeviceTime,
        )
        overlay = BlockOverlayController(
            context = this,
            windowManager = getSystemService(WindowManager::class.java),
            handler = Handler(Looper.getMainLooper()),
            onDismissed = ::onOverlayDismissed,
        )
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

        val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope = serviceScope
        serviceScope.launch {
            snapshotSource.snapshots()
                .catch { failure -> DetectionLog.lifecycle("snapshot-failed", failure.javaClass.simpleName) }
                .collect { restored ->
                    snapshot = restored
                    DetectionLog.lifecycle(
                        state = "snapshot",
                        detail = "targets=${restored.packageToApp.size} " +
                            "apps=${restored.rulesByApp.size} grants=${restored.grantsByApp.size}",
                    )
                }
        }

        DetectionLog.lifecycle("connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val activePipeline = pipeline ?: return
        val current = snapshot

        val result = activePipeline.onWindowStateChanged(
            packageName = event.packageName?.toString(),
            className = event.className?.toString(),
            snapshot = current,
        )

        DetectionLog.decision(result, SystemClock.uptimeMillis() - event.eventTime)

        if (result.transition.movesTheForeground()) {
            overlay?.dismissIfForegroundLeft(result.app)
        }

        if (result.shouldBlock && result.app != null && result.decision != null) {
            showBlock(result.app, result.decision, current)
        }
    }

    private fun showBlock(app: AppRef, decision: RestrictionDecision, current: ProtectionSnapshot) {
        val presentation = presentationFor(app, decision, current)
        reasonsWhenShown = decision.allReasons.ifEmpty { listOf(RuleMode.ALWAYS) }
        overlay?.show(app) {
            BlockSocialTheme {
                BlockScreen(
                    presentation = presentation,
                    onStayFocused = {
                        overlay?.dismiss(BlockOutcome.STAYED_FOCUSED)
                        performGlobalAction(GLOBAL_ACTION_HOME)
                    },
                    onOpenTemporarily = { overlay?.dismiss(BlockOutcome.OPENED_TEMPORARILY) },
                )
            }
        }
    }

    private fun presentationFor(
        app: AppRef,
        decision: RestrictionDecision,
        current: ProtectionSnapshot,
    ): BlockPresentation {
        val at = readDeviceTime()
        val counts = current.todayCounts[app] ?: BlockCounts.None
        return BlockPresentation(
            app = app,
            appDisplayName = current.displayNames[app] ?: app.value,
            primaryReason = decision.primaryReason ?: RuleMode.ALWAYS,
            activeUntil = scheduleEndFor(app, decision, current, at),
            zone = at.zone,
            opensToday = counts.opens + 1,
            endedHereToday = counts.endedHere,
        )
    }

    private fun scheduleEndFor(
        app: AppRef,
        decision: RestrictionDecision,
        current: ProtectionSnapshot,
        at: DeviceTime,
    ): Instant? {
        if (decision.primaryReason != RuleMode.SCHEDULE) return null
        return current.rulesByApp[app]
            .orEmpty()
            .filterIsInstance<RestrictionRule.Schedule>()
            .firstNotNullOfOrNull { ScheduleEvaluator.activeUntil(it, at) }
    }

    private fun onOverlayDismissed(app: AppRef, outcome: BlockOutcome) {
        val at = readDeviceTime()
        val reasons = reasonsWhenShown
        val event = BlockEvent(
            id = UUID.randomUUID().toString(),
            restrictedAppRef = app,
            occurredAt = at.wallClock,
            zone = at.zone,
            primaryReason = reasons.first(),
            allReasons = reasons,
            userAction = outcome.userAction,
            bypassDurationMinutes = BypassPolicy.Android.grantDurationMinutes
                .takeIf { outcome.userAction == UserAction.BYPASSED },
            platform = Platform.ANDROID,
        )
        DetectionLog.lifecycle("dismissed", "app=${app.value} outcome=$outcome")
        scope?.launch {
            runCatching { blockEvents.record(event) }
                .onFailure { DetectionLog.lifecycle("record-failed", it.javaClass.simpleName) }
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        overlay?.dismiss(BlockOutcome.SERVICE_STOPPED)
        runCatching { unregisterReceiver(screenOffReceiver) }
        scope?.cancel()
        scope = null
        pipeline = null
        overlay = null
        snapshot = ProtectionSnapshot.Empty
        DetectionLog.lifecycle("unbound")
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

    private fun readDeviceTime(): DeviceTime = DeviceTime(
        wallClock = Instant.now(),
        monotonicMillis = SystemClock.elapsedRealtime(),
        zone = ZoneId.systemDefault(),
    )
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
