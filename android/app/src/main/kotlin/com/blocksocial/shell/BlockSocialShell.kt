package com.blocksocial.shell

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blocksocial.R
import com.blocksocial.core.ui.theme.Radius
import com.blocksocial.core.ui.theme.Spacing

object ShellTags {
    const val BACK = "shell-back"
    fun tab(destination: Destination) = "shell-tab-${destination.encode()}"
    fun screen(destination: Destination) = "shell-screen-${destination.encode()}"
}

@Composable
fun BlockSocialShell(
    navigator: ShellNavigator,
    content: @Composable (Destination) -> Unit,
) {
    val current = navigator.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding(),
    ) {
        if (navigator.canGoBack) {
            TextButton(
                onClick = { navigator.back() },
                modifier = Modifier
                    .padding(horizontal = Spacing.sm)
                    .defaultMinSize(minHeight = 50.dp)
                    .testTag(ShellTags.BACK),
            ) {
                Text(stringResource(R.string.shell_back))
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .testTag(ShellTags.screen(current)),
        ) {
            content(current)
        }

        TabBar(navigator = navigator)
    }
}

@Composable
private fun TabBar(navigator: ShellNavigator) {
    val selected = navigator.selectedTab

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Destination.tabs.forEach { tab ->
            Tab(
                tab = tab,
                selected = tab == selected,
                onClick = { navigator.selectTab(tab) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Tab(
    tab: Destination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(tabLabel(tab))

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 56.dp)
            .padding(vertical = Spacing.sm)
            .testTag(ShellTags.tab(tab))
            .semantics {
                this.selected = selected
                contentDescription = label
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .height(Spacing.xs)
                .fillMaxWidth(INDICATOR_WIDTH)
                .background(
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(Radius.xs),
                ),
        )
    }
}

private const val INDICATOR_WIDTH = 0.5f

@StringRes
private fun tabLabel(tab: Destination): Int = when (tab) {
    Destination.Dashboard -> R.string.shell_tab_today
    Destination.Apps -> R.string.shell_tab_apps
    Destination.History -> R.string.shell_tab_history
    Destination.Protection -> R.string.shell_tab_today
    is Destination.Rules -> R.string.shell_tab_apps
}
