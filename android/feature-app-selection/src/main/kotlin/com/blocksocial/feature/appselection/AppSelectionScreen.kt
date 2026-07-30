package com.blocksocial.feature.appselection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.ui.theme.Numeric
import com.blocksocial.core.ui.theme.Radius
import com.blocksocial.core.ui.theme.Spacing

object AppSelectionTags {
    const val LIST = "selection-list"
    const val EMPTY = "selection-empty"
    fun row(ref: AppRef) = "selection-row-${ref.value}"
    fun toggle(ref: AppRef) = "selection-toggle-${ref.value}"
}

@Composable
fun AppSelectionScreen(
    rows: List<AppSelectionRow>,
    onSelectedChange: (AppRef, Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding()
            .padding(horizontal = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Text(
            text = stringResource(R.string.selection_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = Spacing.xl),
        )
        Text(
            text = stringResource(R.string.selection_explanation),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (rows.none { it.installState == InstallState.INSTALLED }) {
            Text(
                text = stringResource(R.string.selection_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(AppSelectionTags.EMPTY),
            )
        }

        LazyColumn(
            modifier = Modifier.testTag(AppSelectionTags.LIST),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(rows, key = { it.ref.value }) { row ->
                AppRow(row = row, onSelectedChange = onSelectedChange)
            }
        }
    }
}

@Composable
private fun AppRow(row: AppSelectionRow, onSelectedChange: (AppRef, Boolean) -> Unit) {
    val stateWord = when {
        !row.canBeSelected -> stringResource(R.string.selection_not_installed)
        row.selected -> stringResource(R.string.selection_state_selected)
        else -> stringResource(R.string.selection_state_not_selected)
    }
    val description = stringResource(R.string.selection_row_description, row.displayName, stateWord)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AppSelectionTags.row(row.ref))
            .semantics { contentDescription = description }
            .padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppMark(installed = row.canBeSelected)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!row.canBeSelected) {
                Text(
                    text = stringResource(R.string.selection_not_installed),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (row.canBeSelected) {
            Switch(
                checked = row.selected,
                onCheckedChange = { onSelectedChange(row.ref, it) },
                modifier = Modifier.testTag(AppSelectionTags.toggle(row.ref)),
            )
        } else {
            Text(
                text = stringResource(R.string.selection_not_installed_short),
                style = Numeric,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppMark(installed: Boolean) {
    val shape = RoundedCornerShape(Radius.sm)
    val modifier = Modifier.size(Spacing.xxl)
    if (installed) {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, shape))
    } else {
        Box(modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape))
    }
}
