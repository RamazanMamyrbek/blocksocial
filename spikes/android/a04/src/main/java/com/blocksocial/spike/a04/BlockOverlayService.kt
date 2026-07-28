package com.blocksocial.spike.a04

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.runtime.Composable

class BlockOverlayService : AccessibilityService() {

    private var detector: LaunchDetector? = null
    private var overlay: BlockOverlayController? = null
    private var grants: GrantRepository? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        detector = LaunchDetector(
            targetPackages = TARGET_PACKAGES,
            selfPackage = packageName,
            systemPackages = SystemPackages.resolve(this),
            debounceWindowMillis = DEBOUNCE_WINDOW_MILLIS
        )
        overlay = BlockOverlayController(
            context = this,
            windowManager = getSystemService(WindowManager::class.java),
            handler = handler,
            watchdogTimeoutMillis = WATCHDOG_TIMEOUT_MILLIS,
            onDismissed = { packageName, reason ->
                log("overlay dismissed package=$packageName reason=$reason")
            }
        )
        val repository = GrantRepository(SharedPreferencesGrantStorage(this))
        val now = now()
        repository.loadAndPurge(now).forEach { (packageName, evaluation) ->
            log("grant discarded on start package=$packageName evaluation=$evaluation")
        }
        grants = repository
        log(
            "connected targets=$TARGET_PACKAGES grantMillis=$GRANT_DURATION_MILLIS " +
                "activeGrants=${repository.activePackages()} " +
                "wallClock=${now.wallClockMillis} elapsedRealtime=${now.elapsedRealtimeMillis}"
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }
        val activeDetector = detector ?: return
        val activeOverlay = overlay ?: return
        val activeGrants = grants ?: return
        val foregroundPackage = event.packageName?.toString()
        val decision = activeDetector.onForegroundPackageChanged(
            foregroundPackage,
            isActivityWindow(foregroundPackage, event.className?.toString()),
            event.eventTime
        )

        log("package=$foregroundPackage decision=$decision")

        when (decision) {
            TransitionDecision.TARGET_ENTERED -> {
                val packageName = foregroundPackage!!
                val evaluation = activeGrants.evaluate(packageName, now())
                if (evaluation != null && evaluation.suppressesBlock) {
                    log("package=$packageName suppressedBy=$evaluation")
                } else {
                    log("package=$packageName grant=$evaluation blocking")
                    showBlockScreen(packageName, activeOverlay, activeGrants)
                }
            }

            TransitionDecision.IGNORED_SYSTEM,
            TransitionDecision.IGNORED_NOT_TARGET ->
                activeOverlay.dismissIfForegroundLeft(foregroundPackage!!)

            else -> Unit
        }
    }

    private fun showBlockScreen(
        packageName: String,
        controller: BlockOverlayController,
        repository: GrantRepository
    ) {
        val label = applicationLabel(packageName)
        controller.show(packageName) {
            blockScreenContent(packageName, label, controller, repository)
        }
    }

    @Composable
    private fun blockScreenContent(
        packageName: String,
        label: String,
        controller: BlockOverlayController,
        repository: GrantRepository
    ) {
        BlockScreen(
            appLabel = getString(R.string.block_app_label, label),
            ruleHeadline = getString(R.string.block_headline),
            supportingText = getString(R.string.block_supporting),
            footnote = getString(R.string.block_footnote),
            stayFocusedLabel = getString(R.string.block_stay_focused),
            openTemporarilyLabel = getString(
                R.string.block_open_temporarily,
                GRANT_DURATION_MILLIS / 1000
            ),
            onStayFocused = {
                controller.dismiss(OverlayDismissReason.STAY_FOCUSED)
                performGlobalAction(GLOBAL_ACTION_HOME)
            },
            onOpenTemporarily = {
                val grant = repository.grant(packageName, GRANT_DURATION_MILLIS, now())
                log(
                    "grant created package=$packageName " +
                        "expiresAtWallClock=${grant.expiresAtWallClockMillis} " +
                        "expiresAtElapsedRealtime=${grant.expiresAtElapsedRealtimeMillis}"
                )
                controller.dismiss(OverlayDismissReason.OPEN_TEMPORARILY)
            }
        )
    }

    private fun now() = DeviceTime(
        wallClockMillis = System.currentTimeMillis(),
        elapsedRealtimeMillis = SystemClock.elapsedRealtime()
    )

    private fun applicationLabel(packageName: String): String = runCatching {
        packageManager.getApplicationLabel(
            packageManager.getApplicationInfo(packageName, 0)
        ).toString()
    }.getOrDefault(packageName)

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
        overlay?.dismiss(OverlayDismissReason.SERVICE_STOPPED)
        overlay = null
        detector = null
        grants = null
        log("unbound")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        overlay?.dismiss(OverlayDismissReason.SERVICE_STOPPED)
        overlay = null
        super.onDestroy()
    }

    private fun log(message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, message)
        }
    }

    private companion object {
        const val TAG = "SpikeA04"
        const val DEBOUNCE_WINDOW_MILLIS = 1_000L
        const val WATCHDOG_TIMEOUT_MILLIS = 30_000L
        const val GRANT_DURATION_MILLIS = 60_000L
        val TARGET_PACKAGES = setOf("com.google.android.youtube", "com.android.chrome")
    }
}
