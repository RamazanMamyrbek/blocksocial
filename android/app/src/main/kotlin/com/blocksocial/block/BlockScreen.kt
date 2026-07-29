package com.blocksocial.block

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blocksocial.R
import com.blocksocial.core.model.RuleMode
import com.blocksocial.core.ui.theme.Numeric
import com.blocksocial.core.ui.theme.Spacing
import java.time.format.DateTimeFormatter

object BlockScreenTags {
    const val HEADLINE = "block-headline"
    const val STAY_FOCUSED = "block-stay-focused"
    const val OPEN_TEMPORARILY = "block-open-temporarily"
    const val FOOTNOTE = "block-footnote"
}

@Composable
fun BlockScreen(
    presentation: BlockPresentation,
    onStayFocused: () -> Unit,
    onOpenTemporarily: () -> Unit,
) {
    val headline = headlineFor(presentation)

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
            text = stringResource(R.string.block_app_line, presentation.appDisplayName),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = headline,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(BlockScreenTags.HEADLINE),
        )

        Text(
            text = stringResource(R.string.block_supporting),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = onStayFocused,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .testTag(BlockScreenTags.STAY_FOCUSED),
        ) {
            Text(
                text = stringResource(R.string.block_stay_focused),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        OutlinedButton(
            onClick = onOpenTemporarily,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .testTag(BlockScreenTags.OPEN_TEMPORARILY),
        ) {
            Text(
                text = stringResource(R.string.block_open_temporarily),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Text(
            text = stringResource(
                R.string.block_footnote,
                presentation.endedHereToday,
                presentation.opensToday,
            ),
            style = Numeric,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(BlockScreenTags.FOOTNOTE),
        )
    }
}

@Composable
private fun headlineFor(presentation: BlockPresentation): String {
    val formatter = remember(presentation.zone) {
        DateTimeFormatter.ofPattern("HH:mm").withZone(presentation.zone)
    }
    return when (presentation.primaryReason) {
        RuleMode.ALWAYS, RuleMode.FOCUS_SESSION -> stringResource(R.string.block_headline_always)
        RuleMode.DAILY_LIMIT -> stringResource(R.string.block_headline_daily_limit)
        RuleMode.SCHEDULE -> presentation.activeUntil
            ?.let { stringResource(R.string.block_headline_schedule_until, formatter.format(it)) }
            ?: stringResource(R.string.block_headline_schedule)
    }
}
