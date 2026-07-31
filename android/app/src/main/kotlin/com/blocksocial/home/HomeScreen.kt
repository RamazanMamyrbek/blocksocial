package com.blocksocial.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.blocksocial.R
import com.blocksocial.core.domain.ProtectionBanner
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.ui.component.IconBadge
import com.blocksocial.core.ui.component.SectionHeader
import com.blocksocial.core.ui.component.SoftCard
import com.blocksocial.core.ui.component.SoftRow
import com.blocksocial.core.ui.icon.BlockSocialIcons
import com.blocksocial.core.ui.theme.LocalBlockSocialPalette
import com.blocksocial.core.ui.theme.Spacing
import com.blocksocial.feature.rules.ruleSummary

object HomeTags {
    const val PROTECTION = "home-protection"
    const val ADD = "home-add"
    const val EMPTY = "home-empty"
    const val TODAY = "home-today"
    fun app(ref: AppRef) = "home-app-${ref.value}"
    fun toggle(ref: AppRef) = "home-toggle-${ref.value}"
}

@Composable
fun HomeScreen(
    state: HomeState,
    onOpenProtection: () -> Unit,
    onOpenApp: (RestrictedAppRow) -> Unit,
    onPausedChange: (RestrictedAppRow, Boolean) -> Unit,
    onAddApp: () -> Unit,
    onOpenToday: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        ProtectionCard(banner = state.protection, onClick = onOpenProtection)

        SectionHeader(stringResource(R.string.home_your_limits))

        if (state.nothingRestrictedYet) {
            SoftCard(modifier = Modifier.testTag(HomeTags.EMPTY)) {
                Text(
                    text = stringResource(R.string.home_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        state.apps.forEach { row ->
            AppRow(
                row = row,
                onOpen = { onOpenApp(row) },
                onPausedChange = { paused -> onPausedChange(row, paused) },
            )
        }

        Button(
            onClick = onAddApp,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .testTag(HomeTags.ADD),
        ) {
            Icon(
                imageVector = BlockSocialIcons.Plus,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.home_add_app),
                modifier = Modifier.padding(start = Spacing.sm),
            )
        }

        SoftRow(modifier = Modifier.testTag(HomeTags.TODAY), onClick = onOpenToday) {
            IconBadge(
                icon = BlockSocialIcons.Check,
                tint = LocalBlockSocialPalette.current.success,
                background = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            Text(
                text = stringResource(
                    R.string.home_today_summary,
                    state.stayedFocusedToday,
                    state.interventionsToday,
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = BlockSocialIcons.Chevron,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ProtectionCard(banner: ProtectionBanner, onClick: () -> Unit) {
    val palette = LocalBlockSocialPalette.current
    val working = banner == ProtectionBanner.RUNNING
    val headline = stringResource(protectionHeadline(banner))
    val detail = stringResource(protectionDetail(banner))

    SoftRow(modifier = Modifier.testTag(HomeTags.PROTECTION), onClick = onClick) {
        IconBadge(
            icon = if (working) BlockSocialIcons.ShieldCheck else BlockSocialIcons.Warning,
            tint = if (working) palette.success else palette.warning,
            background = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = headline,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = BlockSocialIcons.Chevron,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun AppRow(
    row: RestrictedAppRow,
    onOpen: () -> Unit,
    onPausedChange: (Boolean) -> Unit,
) {
    val palette = LocalBlockSocialPalette.current
    val summary = when {
        !row.installed -> stringResource(R.string.home_not_installed)
        row.rules.isEmpty() -> stringResource(R.string.home_no_rule_yet)
        row.paused -> stringResource(R.string.home_paused)
        else -> ruleSummary(row.rules.first { it.enabled })
    }
    val description = stringResource(R.string.home_app_description, row.displayName, summary)

    SoftRow(
        modifier = Modifier
            .testTag(HomeTags.app(row.ref))
            .semantics { contentDescription = description },
        onClick = onOpen,
    ) {
        IconBadge(
            icon = if (row.paused) BlockSocialIcons.Circle else BlockSocialIcons.Pause,
            tint = if (row.paused) MaterialTheme.colorScheme.onSurfaceVariant else palette.primary,
            background = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
        Column(modifier = Modifier.weight(1f)) {
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
        }
        if (row.rules.isNotEmpty()) {
            Switch(
                checked = !row.paused,
                onCheckedChange = { on -> onPausedChange(!on) },
                modifier = Modifier.testTag(HomeTags.toggle(row.ref)),
            )
        } else {
            Icon(
                imageVector = BlockSocialIcons.Chevron,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@StringRes
private fun protectionHeadline(banner: ProtectionBanner): Int = when (banner) {
    ProtectionBanner.RUNNING -> R.string.home_protection_running
    ProtectionBanner.NEVER_SET_UP -> R.string.home_protection_never_set_up
    ProtectionBanner.STOPPED -> R.string.home_protection_stopped
    ProtectionBanner.STOPPED_SINCE_LAST_OPEN -> R.string.home_protection_stopped_since
}

@StringRes
private fun protectionDetail(banner: ProtectionBanner): Int = when (banner) {
    ProtectionBanner.RUNNING -> R.string.home_protection_running_detail
    ProtectionBanner.NEVER_SET_UP -> R.string.home_protection_set_up_detail
    ProtectionBanner.STOPPED, ProtectionBanner.STOPPED_SINCE_LAST_OPEN ->
        R.string.home_protection_stopped_detail
}
