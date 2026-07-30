package com.blocksocial.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.blocksocial.core.domain.ProtectionRequirement
import com.blocksocial.core.domain.RequirementHealth
import com.blocksocial.core.domain.RequirementStatus
import com.blocksocial.core.ui.theme.LocalBlockSocialPalette
import com.blocksocial.core.ui.theme.Radius
import com.blocksocial.core.ui.theme.Spacing

object ProtectionHealthTags {
    const val SUMMARY = "health-summary"
    const val OEM = "health-oem"
    fun item(requirement: ProtectionRequirement) = "health-item-${requirement.name}"
    fun mark(requirement: ProtectionRequirement) = "health-mark-${requirement.name}"
    fun repair(requirement: ProtectionRequirement) = "health-repair-${requirement.name}"
}

@Composable
fun ProtectionHealthScreen(
    items: List<RequirementHealth>,
    blockingRuns: Boolean,
    onRepair: (ProtectionRequirement) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        Text(
            text = stringResource(R.string.health_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = if (blockingRuns) {
                stringResource(R.string.health_running)
            } else {
                stringResource(R.string.health_not_running)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(ProtectionHealthTags.SUMMARY),
        )

        items.forEach { item -> HealthItem(item = item, onRepair = onRepair) }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = stringResource(R.string.health_oem_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.health_oem_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(ProtectionHealthTags.OEM),
            )
        }
    }
}

@Composable
private fun HealthItem(item: RequirementHealth, onRepair: (ProtectionRequirement) -> Unit) {
    val name = requirementName(item.requirement)
    val statusWord = statusWord(item.status)
    val explanation = explanation(item)
    val description = stringResource(R.string.health_item_description, name, statusWord)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(Radius.md))
            .padding(Spacing.lg)
            .testTag(ProtectionHealthTags.item(item.requirement))
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusMark(status = item.status, tag = ProtectionHealthTags.mark(item.requirement))
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = statusWord,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (explanation != null) {
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { onRepair(item.requirement) },
                modifier = Modifier.testTag(ProtectionHealthTags.repair(item.requirement)),
            ) {
                Text(repairLabel(item.requirement))
            }
        }
    }
}

@Composable
private fun StatusMark(status: RequirementStatus, tag: String) {
    val palette = LocalBlockSocialPalette.current
    val modifier = Modifier.size(Spacing.lg).testTag(tag)
    when (status) {
        RequirementStatus.HEALTHY -> Box(
            modifier = modifier.background(palette.success, RoundedCornerShape(Radius.xs)),
        )

        RequirementStatus.ENABLED_BUT_NOT_RUNNING, RequirementStatus.RUNNING_BUT_SILENT -> Box(
            modifier = modifier.background(palette.warning, TriangleShape),
        )

        RequirementStatus.DENIED -> Box(
            modifier = modifier.border(2.dp, palette.warning, CircleShape),
        )

        RequirementStatus.NOT_ASKED -> Box(
            modifier = modifier.border(1.dp, palette.outline, RoundedCornerShape(Radius.xs)),
        )
    }
}

private val TriangleShape = androidx.compose.foundation.shape.GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

@Composable
private fun requirementName(requirement: ProtectionRequirement): String = when (requirement) {
    ProtectionRequirement.ACCESSIBILITY_SERVICE -> stringResource(R.string.health_accessibility)
    ProtectionRequirement.USAGE_ACCESS -> stringResource(R.string.health_usage)
    ProtectionRequirement.NOTIFICATIONS -> stringResource(R.string.health_notifications)
}

@Composable
private fun statusWord(status: RequirementStatus): String = when (status) {
    RequirementStatus.HEALTHY -> stringResource(R.string.health_status_healthy)
    RequirementStatus.NOT_ASKED -> stringResource(R.string.health_status_not_asked)
    RequirementStatus.DENIED -> stringResource(R.string.health_status_denied)
    RequirementStatus.ENABLED_BUT_NOT_RUNNING -> stringResource(R.string.health_status_not_running)
    RequirementStatus.RUNNING_BUT_SILENT -> stringResource(R.string.health_status_silent)
}

@Composable
private fun repairLabel(requirement: ProtectionRequirement): String = when (requirement) {
    ProtectionRequirement.ACCESSIBILITY_SERVICE -> stringResource(R.string.health_repair_accessibility)
    ProtectionRequirement.USAGE_ACCESS -> stringResource(R.string.health_repair_usage)
    ProtectionRequirement.NOTIFICATIONS -> stringResource(R.string.health_repair_notifications)
}

@Composable
private fun explanation(item: RequirementHealth): String? = when (item.requirement) {
    ProtectionRequirement.ACCESSIBILITY_SERVICE -> when (item.status) {
        RequirementStatus.HEALTHY -> null
        RequirementStatus.NOT_ASKED -> stringResource(R.string.health_accessibility_not_asked)
        RequirementStatus.DENIED -> stringResource(R.string.health_accessibility_denied)
        RequirementStatus.ENABLED_BUT_NOT_RUNNING ->
            stringResource(R.string.health_accessibility_not_running)
        RequirementStatus.RUNNING_BUT_SILENT -> stringResource(R.string.health_accessibility_silent)
    }

    ProtectionRequirement.USAGE_ACCESS ->
        if (item.status == RequirementStatus.HEALTHY) null
        else stringResource(R.string.health_usage_denied)

    ProtectionRequirement.NOTIFICATIONS ->
        if (item.status == RequirementStatus.HEALTHY) null
        else stringResource(R.string.health_notifications_denied)
}
