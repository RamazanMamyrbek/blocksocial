package com.blocksocial.lite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.blocksocial.lite.R
import com.blocksocial.lite.data.LimitStore
import com.blocksocial.lite.domain.LimitStatus

object AppLimitTags {
    const val USED = "limit-used"
    const val REMAINING = "limit-remaining"
    const val FIELD = "limit-field"
    const val ERROR = "limit-error"
    const val SAVE = "limit-save"
    const val REMOVE = "limit-remove"
}

fun minutesTypedIn(text: String): Int? = text.trim().toIntOrNull()
    ?.takeIf { it in LimitStore.MINIMUM_MINUTES..LimitStore.MAXIMUM_MINUTES }

@Composable
fun AppLimitScreen(
    displayName: String,
    status: LimitStatus?,
    typedMinutes: String,
    onTypedMinutesChange: (String) -> Unit,
    onSave: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    val accepted = minutesTypedIn(typedMinutes)

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

        OutlinedTextField(
            value = typedMinutes,
            onValueChange = { typed -> onTypedMinutesChange(typed.filter { it.isDigit() }.take(4)) },
            label = { Text(stringResource(R.string.limit_minutes_per_day)) },
            suffix = { Text(stringResource(R.string.limit_minutes_suffix)) },
            singleLine = true,
            isError = typedMinutes.isNotEmpty() && accepted == null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            textStyle = TextStyle(fontFamily = Numeric.fontFamily),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(AppLimitTags.FIELD),
        )

        if (typedMinutes.isNotEmpty() && accepted == null) {
            Text(
                text = stringResource(
                    R.string.limit_out_of_range,
                    LimitStore.MINIMUM_MINUTES,
                    LimitStore.MAXIMUM_MINUTES,
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = LocalBlockSocialPalette.current.danger,
                modifier = Modifier.testTag(AppLimitTags.ERROR),
            )
        }

        Text(
            text = stringResource(R.string.limit_starts_now),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = { accepted?.let(onSave) },
            enabled = accepted != null,
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
fun timeOf(minutes: Int): String = when {
    minutes < 60 -> stringResource(R.string.duration_minutes, minutes)
    minutes % 60 == 0 -> stringResource(R.string.duration_hours, minutes / 60)
    else -> stringResource(R.string.duration_hours_minutes, minutes / 60, minutes % 60)
}
