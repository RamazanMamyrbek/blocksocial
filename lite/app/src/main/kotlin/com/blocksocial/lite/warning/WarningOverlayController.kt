package com.blocksocial.lite.warning

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import com.blocksocial.lite.R

class WarningOverlayController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onDismissed: () -> Unit,
) {

    private var host: OverlayViewHost? = null
    private var shownFor: String? = null

    val isShowing: Boolean get() = host != null

    fun show(app: String, content: @Composable () -> Unit) {
        if (host != null) {
            if (shownFor == app) return
            dismiss()
        }
        val newHost = OverlayViewHost(context) { dismiss() }
        newHost.composeView.setContent(content)
        windowManager.addView(newHost, layoutParams())
        newHost.onAttachedToWindowManager()
        host = newHost
        shownFor = app
    }

    fun dismiss() {
        val attached = host ?: return
        host = null
        shownFor = null
        attached.onDetachedFromWindowManager()
        runCatching { windowManager.removeView(attached) }
        onDismissed()
    }

    fun dismissIfForegroundLeft(app: String?) {
        if (host != null && shownFor != app) dismiss()
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
        params.title = context.getString(R.string.warning_window_title)
        return params
    }
}
