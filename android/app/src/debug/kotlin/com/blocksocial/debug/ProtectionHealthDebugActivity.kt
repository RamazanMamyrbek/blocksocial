package com.blocksocial.debug

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.blocksocial.core.domain.NotificationAsk
import com.blocksocial.core.domain.ProtectionRequirement
import com.blocksocial.core.ui.theme.BlockSocialTheme
import com.blocksocial.core.ui.theme.Spacing
import com.blocksocial.feature.onboarding.ProtectionHealthScreen
import com.blocksocial.health.ProtectionHealth
import com.blocksocial.health.ProtectionHealthProbe
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProtectionHealthDebugActivity : ComponentActivity() {

    @Inject
    lateinit var probe: ProtectionHealthProbe

    private var health by mutableStateOf(ProtectionHealth.Unknown)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlockSocialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .safeDrawingPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.xl),
                ) {
                    ProtectionHealthScreen(
                        items = health.items,
                        blockingRuns = health.blockingRuns,
                        onRepair = ::repair,
                    )
                }
            }
        }
    }

    private val notificationRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { lifecycleScope.launch { health = probe.read() } }

    override fun onResume() {
        super.onResume()
        probe.startProbe()
        lifecycleScope.launch {
            health = probe.read()
            delay(PROBE_SETTLE_MILLIS)
            health = probe.read()
            askForNotificationsIfNeeded()
        }
    }

    private suspend fun askForNotificationsIfNeeded() {
        if (probe.notificationAsk(health) != NotificationAsk.ASK_NOW) return
        probe.rememberNotificationRequest()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            repair(ProtectionRequirement.NOTIFICATIONS)
        }
    }

    private fun repair(requirement: ProtectionRequirement) {
        val action = when (requirement) {
            ProtectionRequirement.ACCESSIBILITY_SERVICE -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            ProtectionRequirement.USAGE_ACCESS -> Settings.ACTION_USAGE_ACCESS_SETTINGS
            ProtectionRequirement.NOTIFICATIONS -> Settings.ACTION_APP_NOTIFICATION_SETTINGS
        }
        val intent = Intent(action).apply {
            if (requirement == ProtectionRequirement.NOTIFICATIONS) {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        }
        runCatching { startActivity(intent) }
    }

    private companion object {
        const val PROBE_SETTLE_MILLIS = 4_000L
    }
}
