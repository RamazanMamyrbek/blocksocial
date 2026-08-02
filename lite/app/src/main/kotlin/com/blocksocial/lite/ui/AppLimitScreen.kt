package com.blocksocial.lite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.blocksocial.lite.R
import com.blocksocial.lite.data.LimitStore
import com.blocksocial.lite.domain.LimitStatus

object AppLimitTags {
    const val USED = "limit-used"
    const val REMAINING = "limit-remaining"
    const val VALUE = "limit-value"
    const val SAVE = "limit-save"
    const val REMOVE = "limit-remove"
}

@Composable
fun AppLimitScreen(
    displayName: String,
    status: LimitStatus?,
    draftMinutes: Int,
    onDraftChange: (Int) -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = displayName,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (status != null && status.measured) {
            SoftCard {
                Figure(
                    tag = AppLimitTags.USED,
                    label = stringResource(R.string.limit_used_label),
                    value = timeOf(status.usedMinutes ?: 0),
                )
            }
            SoftCard {
                Figure(
                    tag = AppLimitTags.REMAINING,
                    label = stringResource(R.string.limit_remaining_label),
                    value = if (status.reached) {
                        stringResource(R.string.limit_none_left)
                    } else {
                        timeOf(status.remainingMinutes ?: 0)
                    },
                )
            }
        } else {
            SoftCard {
                Text(
                    text = stringResource(
                        if (status == null) R.string.limit_no_limit_yet else R.string.limit_not_measured,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SectionHeader(stringResource(R.string.limit_editor_header))

        SoftRow {
            Text(
                text = stringResource(R.string.limit_minutes_per_day),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Stepper(
                minutes = draftMinutes,
                onChange = onDraftChange,
            )
        }

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .testTag(AppLimitTags.SAVE),
        ) {
            Text(
                text = stringResource(
                    if (status == null) R.string.limit_set else R.string.limit_save,
                ),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        if (status != null) {
            TextButton(
                onClick = onRemove,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AppLimitTags.REMOVE),
            ) {
                Text(
                    text = stringResource(R.string.limit_remove),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun Figure(tag: String, label: String, value: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = value,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag(tag),
    )
}

@Composable
private fun Stepper(minutes: Int, onChange: (Int) -> Unit) {
    val lower = stringResource(R.string.limit_lower)
    val raise = stringResource(R.string.limit_raise)
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalButton(
            onClick = { onChange((minutes - LimitStore.STEP_MINUTES).coerceAtLeast(LimitStore.MINIMUM_MINUTES)) },
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = lower },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) {
            Icon(LiteIcons.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        Text(
            text = minutes.toString(),
            style = Numeric.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp)
                .testTag(AppLimitTags.VALUE),
        )
        FilledTonalButton(
            onClick = { onChange((minutes + LimitStore.STEP_MINUTES).coerceAtMost(LimitStore.MAXIMUM_MINUTES)) },
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = raise },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) {
            Icon(LiteIcons.Plus, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun timeOf(minutes: Int): String = when {
    minutes < 60 -> stringResource(R.string.duration_minutes, minutes)
    minutes % 60 == 0 -> stringResource(R.string.duration_hours, minutes / 60)
    else -> stringResource(R.string.duration_hours_minutes, minutes / 60, minutes % 60)
}
