package com.blocksocial.lite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.blocksocial.lite.R
import com.blocksocial.lite.domain.LimitStatus

object HomeTags {
    const val PERMISSIONS = "home-permissions"
    const val EMPTY = "home-empty"
    fun app(id: String) = "home-app-$id"
}

data class AppRow(
    val id: String,
    val displayName: String,
    val status: LimitStatus?,
)

data class HomeState(
    val rows: List<AppRow>,
    val protectionWorking: Boolean,
) {
    val limited: List<AppRow> = rows.filter { it.status != null }
    val unlimited: List<AppRow> = rows.filter { it.status == null }

    companion object {
        val Empty = HomeState(rows = emptyList(), protectionWorking = false)
    }
}

@Composable
fun HomeScreen(
    state: HomeState,
    onOpenApp: (AppRow) -> Unit,
    onFixProtection: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (!state.protectionWorking) {
            SoftRow(
                onClick = onFixProtection,
                modifier = Modifier.testTag(HomeTags.PERMISSIONS),
            ) {
                IconBadge(
                    icon = LiteIcons.Warning,
                    tint = LocalBlockSocialPalette.current.warning,
                    background = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_permissions_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.home_permissions_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Chevron()
            }
        }

        if (state.rows.isEmpty()) {
            SoftCard(modifier = Modifier.testTag(HomeTags.EMPTY)) {
                Text(
                    text = stringResource(R.string.home_no_apps),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.limited.isNotEmpty()) {
            SectionHeader(stringResource(R.string.home_with_limit))
            state.limited.forEach { AppRowCard(it, onOpenApp) }
        }

        if (state.unlimited.isNotEmpty()) {
            SectionHeader(stringResource(R.string.home_without_limit))
            state.unlimited.forEach { AppRowCard(it, onOpenApp) }
        }
    }
}

@Composable
private fun AppRowCard(row: AppRow, onOpenApp: (AppRow) -> Unit) {
    val palette = LocalBlockSocialPalette.current
    val status = row.status
    val summary = when {
        status == null -> stringResource(R.string.home_tap_to_set)
        !status.measured -> stringResource(R.string.home_not_measured)
        status.reached -> stringResource(R.string.home_reached, status.usedMinutes ?: 0, status.limitMinutes)
        else -> stringResource(R.string.home_used, status.usedMinutes ?: 0, status.limitMinutes)
    }

    SoftRow(
        onClick = { onOpenApp(row) },
        modifier = Modifier
            .testTag(HomeTags.app(row.id))
            .semantics { contentDescription = "${row.displayName}, $summary" },
    ) {
        IconBadge(
            icon = if (status?.reached == true) LiteIcons.Hourglass else LiteIcons.Clock,
            tint = if (status?.reached == true) palette.danger else palette.primary,
            background = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = row.displayName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (status != null && status.measured) {
                LinearProgressIndicator(
                    progress = {
                        val used = status.usedMinutes ?: 0
                        (used.toFloat() / status.limitMinutes).coerceIn(0f, 1f)
                    },
                    color = if (status.reached) palette.danger else palette.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.xs),
                )
            }
        }
        Chevron()
    }
}
