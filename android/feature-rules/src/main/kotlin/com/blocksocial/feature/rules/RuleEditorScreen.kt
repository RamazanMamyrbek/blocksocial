package com.blocksocial.feature.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blocksocial.core.model.RuleMode
import com.blocksocial.core.ui.text.rememberClockFormatter
import com.blocksocial.core.ui.text.rememberIs24Hour
import com.blocksocial.core.ui.text.rememberWeekOrder
import com.blocksocial.core.ui.theme.Numeric
import com.blocksocial.core.ui.theme.Radius
import com.blocksocial.core.ui.theme.Spacing
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle

object RuleEditorTags {
    const val OVERNIGHT_WORD = "rule-overnight-word"
    const val OVERNIGHT_SENTENCE = "rule-overnight-sentence"
    const val INTERVAL_BAR = "rule-interval-bar"
    const val PROBLEM = "rule-problem"
    const val SAVE = "rule-save"
    const val START = "rule-start"
    const val END = "rule-end"
    fun mode(mode: RuleMode) = "rule-mode-${mode.name}"
    fun day(day: DayOfWeek) = "rule-day-${day.name}"
}

@Composable
fun RuleEditorScreen(
    draft: RuleDraft,
    onDraftChange: (RuleDraft) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        ModeRow(draft = draft, onDraftChange = onDraftChange)

        when (draft.mode) {
            RuleMode.SCHEDULE -> ScheduleFields(draft = draft, onDraftChange = onDraftChange)
            RuleMode.DAILY_LIMIT -> LimitField(draft = draft, onDraftChange = onDraftChange)
            RuleMode.ALWAYS, RuleMode.FOCUS_SESSION -> Unit
        }

        draft.problems().forEach { problem ->
            Text(
                text = problemText(problem, draft),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag(RuleEditorTags.PROBLEM),
            )
        }

        Button(
            onClick = onSave,
            enabled = draft.isValid,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(RuleEditorTags.SAVE),
        ) {
            Text(stringResource(R.string.rule_save))
        }
    }
}

@Composable
private fun ModeRow(draft: RuleDraft, onDraftChange: (RuleDraft) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        listOf(RuleMode.ALWAYS, RuleMode.SCHEDULE, RuleMode.DAILY_LIMIT).forEach { mode ->
            FilterChip(
                selected = draft.mode == mode,
                onClick = { onDraftChange(draft.copy(mode = mode)) },
                label = { Text(modeLabel(mode)) },
                modifier = Modifier.testTag(RuleEditorTags.mode(mode)),
            )
        }
    }
}

@Composable
private fun ScheduleFields(draft: RuleDraft, onDraftChange: (RuleDraft) -> Unit) {
    Text(
        text = stringResource(R.string.rule_days),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
    val locale = LocalLocale.current.platformLocale
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        rememberWeekOrder().forEach { day ->
            val fullName = day.getDisplayName(TextStyle.FULL, locale)
            FilterChip(
                selected = day in draft.daysOfWeek,
                onClick = {
                    val days = if (day in draft.daysOfWeek) draft.daysOfWeek - day else draft.daysOfWeek + day
                    onDraftChange(draft.copy(daysOfWeek = days))
                },
                label = { Text(day.getDisplayName(TextStyle.NARROW, locale)) },
                modifier = Modifier
                    .testTag(RuleEditorTags.day(day))
                    .semantics { contentDescription = fullName },
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        TimeField(
            tag = RuleEditorTags.START,
            label = stringResource(R.string.rule_from),
            value = draft.startLocalTime,
            onChange = { onDraftChange(draft.copy(startLocalTime = it)) },
        )
        TimeField(
            tag = RuleEditorTags.END,
            label = stringResource(R.string.rule_to),
            value = draft.endLocalTime,
            onChange = { onDraftChange(draft.copy(endLocalTime = it)) },
        )
    }

    IntervalSummary(draft.interval)
}

@Composable
fun IntervalSummary(interval: IntervalDescription) {
    val clock = rememberClockFormatter()
    val start = clock(interval.startLocalTime)
    val end = clock(interval.endLocalTime)

    if (interval.crossesMidnight) {
        Text(
            text = stringResource(R.string.rule_overnight),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(RuleEditorTags.OVERNIGHT_WORD),
        )
    }

    IntervalBar(interval)

    Text(
        text = if (interval.crossesMidnight) {
            stringResource(R.string.rule_overnight_explained, start, end)
        } else {
            stringResource(R.string.rule_same_day_explained, start, end)
        },
        style = Numeric,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag(RuleEditorTags.OVERNIGHT_SENTENCE),
    )
}

@Composable
private fun IntervalBar(interval: IntervalDescription) {
    val shape = RoundedCornerShape(Radius.xs)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(RuleEditorTags.INTERVAL_BAR),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (interval.firstSegmentMinutes > 0) {
                Box(
                    modifier = Modifier
                        .weight(interval.firstSegmentMinutes.toFloat())
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary, shape),
                )
            }
            if (interval.crossesMidnight && interval.secondSegmentMinutes > 0) {
                Box(
                    modifier = Modifier
                        .weight(interval.secondSegmentMinutes.toFloat())
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary, shape),
                )
            }
        }
        if (interval.crossesMidnight) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(interval.firstSegmentMinutes.toFloat()))
                Text(
                    text = stringResource(R.string.rule_midnight_marker),
                    style = Numeric,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(interval.secondSegmentMinutes.toFloat().coerceAtLeast(1f)),
                )
            }
        }
    }
}

@Composable
private fun LimitField(draft: RuleDraft, onDraftChange: (RuleDraft) -> Unit) {
    val decreaseDescription = stringResource(R.string.rule_limit_less_description, LIMIT_STEP)
    val increaseDescription = stringResource(R.string.rule_limit_more_description, LIMIT_STEP)

    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.rule_limit_minutes),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedButton(
            onClick = { onDraftChange(draft.copy(dailyLimitMinutes = draft.dailyLimitMinutes - LIMIT_STEP)) },
            modifier = Modifier.semantics {
                contentDescription = decreaseDescription
            },
        ) {
            Text(stringResource(R.string.rule_limit_less, LIMIT_STEP))
        }
        Text(
            text = draft.dailyLimitMinutes.toString(),
            style = Numeric,
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedButton(
            onClick = { onDraftChange(draft.copy(dailyLimitMinutes = draft.dailyLimitMinutes + LIMIT_STEP)) },
            modifier = Modifier.semantics {
                contentDescription = increaseDescription
            },
        ) {
            Text(stringResource(R.string.rule_limit_more, LIMIT_STEP))
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(tag: String, label: String, value: LocalTime, onChange: (LocalTime) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    val clock = rememberClockFormatter()
    val is24Hour = rememberIs24Hour()

    OutlinedButton(onClick = { picking = true }, modifier = Modifier.testTag(tag)) {
        Text(stringResource(R.string.rule_time_field, label, clock(value)))
    }

    if (picking) {
        val state = rememberTimePickerState(value.hour, value.minute, is24Hour)
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    onChange(LocalTime.of(state.hour, state.minute))
                    picking = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { picking = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            text = { TimePicker(state = state) },
        )
    }
}

private const val LIMIT_STEP = 5

@Composable
private fun modeLabel(mode: RuleMode): String = when (mode) {
    RuleMode.ALWAYS, RuleMode.FOCUS_SESSION -> stringResource(R.string.rule_mode_always)
    RuleMode.SCHEDULE -> stringResource(R.string.rule_mode_schedule)
    RuleMode.DAILY_LIMIT -> stringResource(R.string.rule_mode_daily_limit)
}

@Composable
private fun problemText(problem: RuleProblem, draft: RuleDraft): String = when (problem) {
    RuleProblem.NO_DAYS_SELECTED -> stringResource(R.string.rule_problem_no_days)
    RuleProblem.INTERVAL_IS_EMPTY ->
        stringResource(R.string.rule_problem_empty_interval, draft.startLocalTime.toString())
    RuleProblem.LIMIT_OUT_OF_RANGE -> stringResource(
        R.string.rule_problem_limit_range,
        RuleDraft.MINIMUM_LIMIT_MINUTES,
        RuleDraft.MAXIMUM_LIMIT_MINUTES,
    )
}
