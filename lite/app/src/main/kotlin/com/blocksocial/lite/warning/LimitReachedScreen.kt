package com.blocksocial.lite.warning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blocksocial.lite.R
import com.blocksocial.lite.ui.Spacing

object WarningTags {
    const val HEADLINE = "warning-headline"
    const val MEASUREMENT = "warning-measurement"
    const val LEAVE = "warning-leave"
    const val CONTINUE = "warning-continue"
}

@Composable
fun LimitReachedScreen(
    appDisplayName: String,
    usedMinutes: Int,
    limitMinutes: Int,
    onLeave: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Text(
            text = stringResource(R.string.warning_app_line, appDisplayName),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = stringResource(R.string.warning_headline),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(WarningTags.HEADLINE),
        )

        Text(
            text = pluralStringResource(
                R.plurals.warning_measurement,
                usedMinutes,
                usedMinutes,
                appDisplayName,
                limitMinutes,
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(WarningTags.MEASUREMENT),
        )

        Text(
            text = stringResource(R.string.warning_question),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Button(
            onClick = onLeave,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .testTag(WarningTags.LEAVE),
        ) {
            Text(
                text = stringResource(R.string.warning_leave),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        OutlinedButton(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .testTag(WarningTags.CONTINUE),
        ) {
            Text(
                text = stringResource(R.string.warning_continue),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
