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

@Composable
fun PermissionsScreen(
    accessibilityGranted: Boolean,
    usageAccessGranted: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
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
            body = stringResource(R.string.permissions_accessibility_body),
            granted = accessibilityGranted,
            onOpenSettings = onOpenAccessibilitySettings,
        )

        Requirement(
            tag = PermissionsTags.USAGE,
            title = stringResource(R.string.permissions_usage_title),
            body = stringResource(R.string.permissions_usage_body),
            granted = usageAccessGranted,
            onOpenSettings = onOpenUsageAccessSettings,
        )
    }
}

@Composable
private fun Requirement(
    tag: String,
    title: String,
    body: String,
    granted: Boolean,
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
            text = stringResource(if (granted) R.string.permissions_working else R.string.permissions_off),
            style = MaterialTheme.typography.labelLarge,
            color = if (granted) palette.success else palette.warning,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!granted) {
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
