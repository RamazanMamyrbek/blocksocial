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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.blocksocial.core.model.RestrictionRule
import java.time.format.TextStyle
import com.blocksocial.core.model.RuleMode
import com.blocksocial.core.ui.text.rememberClockFormatter
import com.blocksocial.core.ui.text.rememberWeekOrder
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
                        text = ruleSummary(rule),
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
                TextButton(onClick = { onEdit(rule) }) { Text(stringResource(R.string.rule_edit)) }
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
fun ruleSummary(rule: RestrictionRule): String {
    val clock = rememberClockFormatter()
    val locale = LocalLocale.current.platformLocale
    return when (rule) {
        is RestrictionRule.AlwaysOn, is RestrictionRule.FocusSession ->
            stringResource(R.string.rule_mode_always)

        is RestrictionRule.Schedule -> {
            val days = rememberWeekOrder()
                .filter { it in rule.daysOfWeek }
                .joinToString(" ") { it.getDisplayName(TextStyle.SHORT, locale) }
            stringResource(
                R.string.rule_schedule_summary,
                days,
                clock(rule.startLocalTime),
                clock(rule.endLocalTime),
            )
        }

        is RestrictionRule.DailyLimit ->
            stringResource(R.string.rule_daily_limit_summary, rule.limitMinutes)
    }
}
