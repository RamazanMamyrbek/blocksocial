package com.blocksocial.detection

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.blocksocial.core.model.DeviceTime
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class BlockSocialAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var snapshotSource: ProtectionSnapshotSource

    @Volatile
    private var snapshot: ProtectionSnapshot = ProtectionSnapshot.Empty

    private var pipeline: DetectionPipeline? = null
    private var scope: CoroutineScope? = null

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            pipeline?.forgetForeground()
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
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

        val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope = serviceScope
        serviceScope.launch {
            snapshotSource.snapshots().collect { restored ->
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

        val result = activePipeline.onWindowStateChanged(
            packageName = event.packageName?.toString(),
            className = event.className?.toString(),
            snapshot = snapshot,
        )

        DetectionLog.decision(result, SystemClock.uptimeMillis() - event.eventTime)
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        runCatching { unregisterReceiver(screenOffReceiver) }
        scope?.cancel()
        scope = null
        pipeline = null
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
