package com.blocksocial.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.blocksocial.core.ui.theme.Numeric
import com.blocksocial.core.ui.theme.Radius
import com.blocksocial.core.ui.theme.Spacing
import kotlin.math.roundToInt

object DashboardTags {
    const val STAYED = "dashboard-stayed"
    const val BYPASSED = "dashboard-bypassed"
    const val REFUSAL_RATE = "dashboard-refusal-rate"
    const val RULES = "dashboard-rules"
    const val STREAK = "dashboard-streak"
    const val STREAK_RULE = "dashboard-streak-rule"
    const val HOURS_SAVED = "dashboard-hours-saved"
    const val EMPTY = "dashboard-empty"
}

@Composable
fun DashboardScreen(state: DashboardState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = stringResource(R.string.dashboard_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (state.interventionsToday == 0) {
            Text(
                text = stringResource(R.string.dashboard_nothing_yet),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(DashboardTags.EMPTY),
            )
        }

        Tile(
            tag = DashboardTags.STAYED,
            label = stringResource(R.string.dashboard_stayed),
            value = stringResource(
                R.string.dashboard_stayed_value,
                state.stayedFocusedToday,
                state.interventionsToday,
            ),
        )

        Tile(
            tag = DashboardTags.BYPASSED,
            label = stringResource(R.string.dashboard_bypassed),
            value = state.bypassedToday.toString(),
        )

        Tile(
            tag = DashboardTags.REFUSAL_RATE,
            label = stringResource(R.string.dashboard_refusal_rate),
            value = state.refusalRateLastSevenDays
                ?.let { stringResource(R.string.dashboard_refusal_rate_value, (it * 100).roundToInt()) }
                ?: stringResource(R.string.dashboard_refusal_rate_absent),
        )

        Tile(
            tag = DashboardTags.RULES,
            label = stringResource(R.string.dashboard_rules),
            value = stringResource(R.string.dashboard_rules_value, state.activeRules, state.pausedRules),
        )

        Tile(
            tag = DashboardTags.STREAK,
            label = stringResource(R.string.dashboard_streak),
            value = stringResource(R.string.dashboard_streak_value, state.streakDays),
        )

        Text(
            text = stringResource(R.string.dashboard_streak_rule, state.bypassAllowancePerDay),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(DashboardTags.STREAK_RULE),
        )

        Text(
            text = stringResource(R.string.dashboard_hours_saved),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(DashboardTags.HOURS_SAVED),
        )
    }
}

@Composable
private fun Tile(tag: String, label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(Radius.md))
            .padding(Spacing.lg)
            .testTag(tag),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = value,
            style = Numeric,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
