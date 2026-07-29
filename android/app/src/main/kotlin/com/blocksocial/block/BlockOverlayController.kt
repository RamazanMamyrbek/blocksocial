package com.blocksocial.block

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import com.blocksocial.R
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.UserAction

enum class BlockOutcome(val userAction: UserAction) {
    STAYED_FOCUSED(UserAction.STAYED_FOCUSED),
    OPENED_TEMPORARILY(UserAction.BYPASSED),
    BACK_PRESSED(UserAction.STAYED_FOCUSED),
    FOREGROUND_MOVED_ON(UserAction.DISMISSED_BY_SYSTEM),
    WATCHDOG_TIMEOUT(UserAction.DISMISSED_BY_SYSTEM),
    SERVICE_STOPPED(UserAction.DISMISSED_BY_SYSTEM),
}

class BlockOverlayController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val handler: Handler,
    private val watchdogTimeoutMillis: Long = DEFAULT_WATCHDOG_TIMEOUT_MILLIS,
    private val onDismissed: (AppRef, BlockOutcome) -> Unit,
) {

    private var host: OverlayViewHost? = null
    private var shownFor: AppRef? = null

    private val watchdog = Runnable { dismiss(BlockOutcome.WATCHDOG_TIMEOUT) }

    val isShowing: Boolean get() = host != null

    val currentApp: AppRef? get() = shownFor

    fun show(app: AppRef, content: @Composable () -> Unit) {
        if (host != null) {
            if (shownFor == app) return
            dismiss(BlockOutcome.FOREGROUND_MOVED_ON)
        }
        val newHost = OverlayViewHost(context) { dismiss(BlockOutcome.BACK_PRESSED) }
        newHost.composeView.setContent(content)
        windowManager.addView(newHost, layoutParams())
        newHost.onAttachedToWindowManager()
        host = newHost
        shownFor = app
        handler.postDelayed(watchdog, watchdogTimeoutMillis)
    }

    fun dismiss(outcome: BlockOutcome) {
        val attached = host ?: return
        handler.removeCallbacks(watchdog)
        val app = shownFor
        host = null
        shownFor = null
        attached.onDetachedFromWindowManager()
        runCatching { windowManager.removeView(attached) }
        if (app != null) onDismissed(app, outcome)
    }

    fun dismissIfForegroundLeft(app: AppRef?) {
        if (host != null && shownFor != app) dismiss(BlockOutcome.FOREGROUND_MOVED_ON)
    }

    private fun layoutParams(): WindowManager.LayoutParams {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.title = context.getString(R.string.block_window_title)
        return params
    }

    companion object {
        const val DEFAULT_WATCHDOG_TIMEOUT_MILLIS = 120_000L
    }
}
