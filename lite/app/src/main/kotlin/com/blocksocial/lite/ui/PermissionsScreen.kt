package com.blocksocial.lite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blocksocial.lite.R

object PermissionsTags {
    const val ACCESSIBILITY = "permissions-accessibility"
    const val USAGE = "permissions-usage"
}

enum class ServiceState { WORKING, SWITCHED_ON_BUT_STOPPED, OFF }

fun serviceStateOf(switchedOn: Boolean, running: Boolean): ServiceState = when {
    running -> ServiceState.WORKING
    switchedOn -> ServiceState.SWITCHED_ON_BUT_STOPPED
    else -> ServiceState.OFF
}

@Composable
fun PermissionsScreen(
    serviceState: ServiceState,
    usageAccessGranted: Boolean,
    overlayFailure: String?,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = stringResource(R.string.permissions_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Requirement(
            tag = PermissionsTags.ACCESSIBILITY,
            title = stringResource(R.string.permissions_accessibility_title),
            state = when (serviceState) {
                ServiceState.WORKING -> stringResource(R.string.permissions_working)
                ServiceState.SWITCHED_ON_BUT_STOPPED -> stringResource(R.string.permissions_stopped)
                ServiceState.OFF -> stringResource(R.string.permissions_off)
            },
            good = serviceState == ServiceState.WORKING,
            body = stringResource(R.string.permissions_accessibility_body),
            extra = when (serviceState) {
                ServiceState.SWITCHED_ON_BUT_STOPPED -> stringResource(R.string.permissions_stopped_advice)
                else -> null
            },
            actionNeeded = serviceState != ServiceState.WORKING,
            onOpenSettings = onOpenAccessibilitySettings,
        )

        Requirement(
            tag = PermissionsTags.USAGE,
            title = stringResource(R.string.permissions_usage_title),
            state = stringResource(
                if (usageAccessGranted) R.string.permissions_working else R.string.permissions_off,
            ),
            good = usageAccessGranted,
            body = stringResource(R.string.permissions_usage_body),
            extra = null,
            actionNeeded = !usageAccessGranted,
            onOpenSettings = onOpenUsageAccessSettings,
        )

        if (overlayFailure != null) {
            SoftCard {
                Text(
                    text = stringResource(R.string.permissions_overlay_failed, overlayFailure),
                    style = MaterialTheme.typography.bodyLarge,
                    color = LocalBlockSocialPalette.current.danger,
                )
            }
        }

        SoftRow(onClick = onOpenDiagnostics) {
            Text(
                text = stringResource(R.string.permissions_diagnostics),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Chevron()
        }

        Text(
            text = stringResource(R.string.permissions_no_notifications),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Requirement(
    tag: String,
    title: String,
    state: String,
    good: Boolean,
    body: String,
    extra: String?,
    actionNeeded: Boolean,
    onOpenSettings: () -> Unit,
) {
    val palette = LocalBlockSocialPalette.current
    SoftCard(modifier = Modifier.testTag(tag)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = state,
            style = MaterialTheme.typography.labelLarge,
            color = if (good) palette.success else palette.warning,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (extra != null) {
            Text(
                text = extra,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (actionNeeded) {
            Button(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp),
            ) {
                Text(
                    text = stringResource(R.string.permissions_open_settings),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
