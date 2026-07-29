package com.blocksocial.core.ui.theme

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

data class MotionDurations(
    val blockAppearMillis: Int,
    val stateChangeMillis: Int,
    val sheetMillis: Int,
) {
    companion object {
        val Full = MotionDurations(
            blockAppearMillis = 120,
            stateChangeMillis = 180,
            sheetMillis = 240,
        )

        val Reduced = MotionDurations(
            blockAppearMillis = 0,
            stateChangeMillis = 0,
            sheetMillis = 0,
        )

        fun forAnimatorDurationScale(scale: Float): MotionDurations =
            if (scale == 0f) Reduced else Full
    }
}

val LocalMotionDurations = staticCompositionLocalOf { MotionDurations.Full }

private fun animatorDurationScale(resolver: ContentResolver): Float =
    Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

@Composable
internal fun rememberMotionDurations(): MotionDurations {
    val resolver = LocalContext.current.contentResolver
    var durations by remember(resolver) {
        mutableStateOf(MotionDurations.forAnimatorDurationScale(animatorDurationScale(resolver)))
    }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                durations = MotionDurations.forAnimatorDurationScale(animatorDurationScale(resolver))
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return durations
}
