package com.blocksocial.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.blocksocial.core.model.BlockEvent
import com.blocksocial.core.model.RuleMode
import com.blocksocial.core.model.UserAction
import com.blocksocial.core.ui.text.rememberEventFormatter
import com.blocksocial.core.ui.theme.Numeric
import com.blocksocial.core.ui.theme.Radius
import com.blocksocial.core.ui.theme.Spacing

object HistoryTags {
    const val LIST = "history-list"
    const val EMPTY = "history-empty"
    fun row(id: String) = "history-row-$id"
    fun mark(id: String) = "history-mark-$id"
}

@Composable
fun HistoryScreen(events: List<BlockEvent>, displayNameOf: (BlockEvent) -> String) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (events.isEmpty()) {
            Text(
                text = stringResource(R.string.history_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(HistoryTags.EMPTY),
            )
        }

        LazyColumn(
            modifier = Modifier.testTag(HistoryTags.LIST),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(events, key = { it.id }) { event ->
                HistoryRow(event = event, displayName = displayNameOf(event))
            }
        }
    }
}

@Composable
private fun HistoryRow(event: BlockEvent, displayName: String) {
    val outcome = outcomeLabel(event.userAction)
    val reason = reasonLabel(event.primaryReason)
    val time = rememberEventFormatter(event.zone)(event.occurredAt)
    val description = stringResource(R.string.history_row_description, displayName, outcome, time)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(HistoryTags.row(event.id))
            .semantics { contentDescription = description }
            .padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutcomeMark(action = event.userAction, tag = HistoryTags.mark(event.id))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.history_outcome_and_reason, outcome, reason),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = time,
            style = Numeric,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OutcomeMark(action: UserAction, tag: String) {
    val size = Modifier.size(Spacing.lg).testTag(tag)
    when (action) {
        UserAction.STAYED_FOCUSED -> Box(
            modifier = size.background(
                MaterialTheme.colorScheme.onSurface,
                RoundedCornerShape(Radius.xs),
            ),
        )

        UserAction.BYPASSED -> Box(
            modifier = size.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
        )

        UserAction.DISMISSED_BY_SYSTEM, UserAction.UNKNOWN -> Box(
            modifier = size.border(
                1.dp,
                MaterialTheme.colorScheme.outline,
                RoundedCornerShape(Radius.xs),
            ),
        )
    }
}

@Composable
private fun outcomeLabel(action: UserAction): String = when (action) {
    UserAction.STAYED_FOCUSED -> stringResource(R.string.history_stayed)
    UserAction.BYPASSED -> stringResource(R.string.history_bypassed)
    UserAction.DISMISSED_BY_SYSTEM, UserAction.UNKNOWN -> stringResource(R.string.history_unresolved)
}

@Composable
private fun reasonLabel(reason: RuleMode): String = when (reason) {
    RuleMode.ALWAYS, RuleMode.FOCUS_SESSION -> stringResource(R.string.history_reason_always)
    RuleMode.SCHEDULE -> stringResource(R.string.history_reason_schedule)
    RuleMode.DAILY_LIMIT -> stringResource(R.string.history_reason_daily_limit)
}
