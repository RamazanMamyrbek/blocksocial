package com.blocksocial.spike.a04

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable

enum class OverlayDismissReason {
    STAY_FOCUSED,
    OPEN_TEMPORARILY,
    BACK_PRESSED,
    PACKAGE_CHANGED,
    WATCHDOG_TIMEOUT,
    SERVICE_STOPPED
}

class BlockOverlayController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val handler: Handler,
    private val watchdogTimeoutMillis: Long,
    private val onDismissed: (String, OverlayDismissReason) -> Unit
) {

    private var host: OverlayViewHost? = null
    private var shownForPackage: String? = null

    private val watchdog = Runnable { dismiss(OverlayDismissReason.WATCHDOG_TIMEOUT) }

    val isShowing: Boolean get() = host != null

    val currentPackage: String? get() = shownForPackage

    fun show(packageName: String, content: @Composable () -> Unit) {
        if (host != null) {
            if (shownForPackage == packageName) {
                return
            }
            dismiss(OverlayDismissReason.PACKAGE_CHANGED)
        }
        val newHost = OverlayViewHost(context) { dismiss(OverlayDismissReason.BACK_PRESSED) }
        newHost.composeView.setContent(content)
        windowManager.addView(newHost, layoutParams())
        newHost.onAttachedToWindowManager()
        host = newHost
        shownForPackage = packageName
        handler.postDelayed(watchdog, watchdogTimeoutMillis)
    }

    fun dismiss(reason: OverlayDismissReason) {
        val attached = host ?: return
        handler.removeCallbacks(watchdog)
        val packageName = shownForPackage
        host = null
        shownForPackage = null
        attached.onDetachedFromWindowManager()
        runCatching { windowManager.removeView(attached) }
        if (packageName != null) {
            onDismissed(packageName, reason)
        }
    }

    fun dismissIfForegroundLeft(packageName: String) {
        if (host != null && shownForPackage != packageName) {
            dismiss(OverlayDismissReason.PACKAGE_CHANGED)
        }
    }

    private fun layoutParams(): WindowManager.LayoutParams {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.title = context.getString(R.string.block_window_title)
        return params
    }
}
