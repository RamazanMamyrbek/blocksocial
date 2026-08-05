package com.blocksocial.lite.detection

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.blocksocial.lite.MainActivity
import com.blocksocial.lite.R
import com.blocksocial.lite.container
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

enum class GuardDecision { RUN, STOP }

fun guardDecisionFor(limitCount: Int, guardEnabled: Boolean): GuardDecision =
    if (guardEnabled && limitCount > 0) GuardDecision.RUN else GuardDecision.STOP

class LimitGuardService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, notification())
        return START_STICKY
    }

    private fun notification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager != null) {
            manager.deleteNotificationChannel(AUDIBLE_CHANNEL_ID)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.guard_channel_name),
                    NotificationManager.IMPORTANCE_MIN,
                ).apply {
                    description = getString(R.string.guard_channel_description)
                    setShowBadge(false)
                },
            )
        }

        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_guard)
            .setContentTitle(getString(R.string.guard_title))
            .setContentText(getString(R.string.guard_text))
            .setContentIntent(open)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "limits-quiet"
        private const val AUDIBLE_CHANNEL_ID = "limits-running"
        private const val NOTIFICATION_ID = 1

        suspend fun follow(context: Context) {
            val container = context.container
            combine(container.limits.limits, container.guard.enabled) { limits, guardEnabled ->
                guardDecisionFor(limits.size, guardEnabled)
            }
                .distinctUntilChanged()
                .collect { decision ->
                    when (decision) {
                        GuardDecision.RUN -> start(context)
                        GuardDecision.STOP -> stop(context)
                    }
                }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, LimitGuardService::class.java)) }
        }

        private fun start(context: Context) {
            runCatching {
                context.startForegroundService(Intent(context, LimitGuardService::class.java))
            }
        }
    }
}
