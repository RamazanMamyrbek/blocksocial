package com.blocksocial.lite

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.blocksocial.lite.data.LimitStore
import com.blocksocial.lite.detection.LimitAccessibilityService
import com.blocksocial.lite.domain.DailyLimit
import com.blocksocial.lite.ui.AppLimitScreen
import com.blocksocial.lite.ui.AppRow
import com.blocksocial.lite.ui.HomeScreen
import com.blocksocial.lite.ui.HomeState
import com.blocksocial.lite.ui.LiteTheme
import com.blocksocial.lite.ui.PermissionsScreen
import com.blocksocial.lite.ui.Spacing
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiteTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .safeDrawingPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.xl),
                ) {
                    LiteApp(
                        container = container,
                        accessibilityRunning = ::accessibilityRunning,
                        openAccessibilitySettings = { open(Settings.ACTION_ACCESSIBILITY_SETTINGS) },
                        openUsageAccessSettings = { open(Settings.ACTION_USAGE_ACCESS_SETTINGS) },
                    )
                }
            }
        }
    }

    private fun open(action: String) {
        runCatching { startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    private fun accessibilityRunning(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val name = LimitAccessibilityService::class.java.name
        return enabled.orEmpty().split(':').any { it.endsWith(name) }
    }
}

private sealed interface Screen {
    data object Home : Screen
    data object Permissions : Screen
    data class Limit(val appId: String) : Screen
}

@Composable
private fun LiteApp(
    container: Container,
    accessibilityRunning: () -> Boolean,
    openAccessibilitySettings: () -> Unit,
    openUsageAccessSettings: () -> Unit,
) {
    val work = rememberCoroutineScope()

    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var draftMinutes by remember { mutableIntStateOf(LimitStore.DEFAULT_MINUTES) }
    var usage by remember { mutableStateOf(container.usage.readToday(container.catalog.packageToApp())) }
    var accessibility by remember { mutableStateOf(accessibilityRunning()) }

    val limits by container.limits.limits.collectAsState(initial = emptyMap())
    val installed = remember { container.catalog.installed() }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            usage = container.usage.readToday(container.catalog.packageToApp())
            accessibility = accessibilityRunning()
        }
    }

    val rows = installed.map { app ->
        AppRow(
            id = app.id,
            displayName = app.displayName,
            status = DailyLimit.statusOf(limits[app.id], usage.minutesFor(app.id)),
        )
    }

    when (val current = screen) {
        Screen.Home -> HomeScreen(
            state = HomeState(
                rows = rows,
                protectionWorking = accessibility && usage.measurementAvailable,
            ),
            onOpenApp = { row ->
                draftMinutes = row.status?.limitMinutes ?: LimitStore.DEFAULT_MINUTES
                screen = Screen.Limit(row.id)
            },
            onFixProtection = { screen = Screen.Permissions },
        )

        Screen.Permissions -> {
            BackHandler { screen = Screen.Home }
            PermissionsScreen(
                accessibilityGranted = accessibility,
                usageAccessGranted = usage.measurementAvailable,
                onOpenAccessibilitySettings = openAccessibilitySettings,
                onOpenUsageAccessSettings = openUsageAccessSettings,
            )
        }

        is Screen.Limit -> {
            BackHandler { screen = Screen.Home }
            val row = rows.first { it.id == current.appId }
            AppLimitScreen(
                displayName = row.displayName,
                status = row.status,
                draftMinutes = draftMinutes,
                onDraftChange = { draftMinutes = it },
                onSave = {
                    work.launch { container.limits.setLimit(current.appId, draftMinutes) }
                    screen = Screen.Home
                },
                onRemove = {
                    work.launch { container.limits.removeLimit(current.appId) }
                    screen = Screen.Home
                },
            )
        }
    }
}
