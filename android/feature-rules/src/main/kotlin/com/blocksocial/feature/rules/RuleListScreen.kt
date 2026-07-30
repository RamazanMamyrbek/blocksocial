package com.blocksocial.feature.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.blocksocial.core.model.RestrictionRule
import com.blocksocial.core.model.RuleMode
import com.blocksocial.core.ui.theme.Numeric
import com.blocksocial.core.ui.theme.Spacing

object RuleListTags {
    const val EMPTY = "rules-empty"
    fun row(id: String) = "rule-row-$id"
    fun toggle(id: String) = "rule-toggle-$id"
    fun state(id: String) = "rule-state-$id"
}

@Composable
fun RuleListScreen(
    rules: List<RestrictionRule>,
    onEnabledChange: (RestrictionRule, Boolean) -> Unit,
    onEdit: (RestrictionRule) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(
            text = stringResource(R.string.rules_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (rules.isEmpty()) {
            Text(
                text = stringResource(R.string.rules_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(RuleListTags.EMPTY),
            )
        }

        rules.forEach { rule ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(RuleListTags.row(rule.id)),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summaryOf(rule),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (rule.enabled) {
                            stringResource(R.string.rules_active)
                        } else {
                            stringResource(R.string.rules_paused)
                        },
                        style = Numeric,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(RuleListTags.state(rule.id)),
                    )
                }
                TextButton(onClick = { onEdit(rule) }) { Text("Edit") }
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = { onEnabledChange(rule, it) },
                    modifier = Modifier.testTag(RuleListTags.toggle(rule.id)),
                )
            }
        }
    }
}

@Composable
private fun summaryOf(rule: RestrictionRule): String = when (rule) {
    is RestrictionRule.AlwaysOn, is RestrictionRule.FocusSession ->
        stringResource(R.string.rule_mode_always)
    is RestrictionRule.Schedule -> {
        val interval = IntervalDescription(rule.startLocalTime, rule.endLocalTime)
        val times = if (interval.crossesMidnight) {
            stringResource(
                R.string.rule_overnight_explained,
                rule.startLocalTime.toString(),
                rule.endLocalTime.toString(),
            )
        } else {
            stringResource(
                R.string.rule_same_day_explained,
                rule.startLocalTime.toString(),
                rule.endLocalTime.toString(),
            )
        }
        "${rule.daysOfWeek.sortedBy { it.value }.joinToString(" ") { it.name.take(2) }} · $times"
    }
    is RestrictionRule.DailyLimit ->
        "${stringResource(R.string.rule_mode_daily_limit)} · ${rule.limitMinutes}"
}
