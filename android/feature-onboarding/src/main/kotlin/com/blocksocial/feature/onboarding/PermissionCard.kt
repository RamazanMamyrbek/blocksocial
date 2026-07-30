package com.blocksocial.feature.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.blocksocial.core.domain.ProtectionRequirement
import com.blocksocial.core.domain.RequirementStatus
import com.blocksocial.core.ui.theme.Numeric
import com.blocksocial.core.ui.theme.Radius
import com.blocksocial.core.ui.theme.Spacing

enum class PermissionSlot { WHAT_IT_ENABLES, WHAT_IS_READ, WHAT_IS_NEVER_READ, HOW_TO_REVOKE }

object PermissionCardTags {
    fun card(requirement: ProtectionRequirement) = "permission-card-${requirement.name}"
    fun slot(requirement: ProtectionRequirement, slot: PermissionSlot) =
        "permission-slot-${requirement.name}-${slot.name}"

    fun action(requirement: ProtectionRequirement) = "permission-action-${requirement.name}"
    fun footnote(requirement: ProtectionRequirement) = "permission-footnote-${requirement.name}"
}

@Composable
fun PermissionCard(
    requirement: ProtectionRequirement,
    status: RequirementStatus,
    onRequest: (ProtectionRequirement) -> Unit,
) {
    val name = stringResource(permissionName(requirement))
    val statusWord = stringResource(permissionStatusWord(status))
    val description = stringResource(R.string.health_item_description, name, statusWord)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(Radius.md))
            .padding(Spacing.lg)
            .testTag(PermissionCardTags.card(requirement))
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = statusWord,
            style = Numeric,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        PermissionSlot.entries.forEach { slot ->
            Text(
                text = stringResource(permissionSlotText(requirement, slot)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(PermissionCardTags.slot(requirement, slot)),
            )
        }

        if (status != RequirementStatus.HEALTHY) {
            Button(
                onClick = { onRequest(requirement) },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .testTag(PermissionCardTags.action(requirement)),
            ) {
                Text(stringResource(permissionActionLabel(requirement)))
            }

            Text(
                text = stringResource(R.string.disclosure_footnote),
                style = Numeric,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(PermissionCardTags.footnote(requirement)),
            )
        }
    }
}

@StringRes
internal fun permissionName(requirement: ProtectionRequirement): Int = when (requirement) {
    ProtectionRequirement.ACCESSIBILITY_SERVICE -> R.string.health_accessibility
    ProtectionRequirement.USAGE_ACCESS -> R.string.health_usage
    ProtectionRequirement.NOTIFICATIONS -> R.string.health_notifications
}

@StringRes
internal fun permissionStatusWord(status: RequirementStatus): Int = when (status) {
    RequirementStatus.HEALTHY -> R.string.health_status_healthy
    RequirementStatus.NOT_ASKED -> R.string.health_status_not_asked
    RequirementStatus.DENIED -> R.string.health_status_denied
    RequirementStatus.ENABLED_BUT_NOT_RUNNING -> R.string.health_status_not_running
    RequirementStatus.RUNNING_BUT_SILENT -> R.string.health_status_silent
}

@StringRes
internal fun permissionActionLabel(requirement: ProtectionRequirement): Int = when (requirement) {
    ProtectionRequirement.ACCESSIBILITY_SERVICE -> R.string.permission_action_accessibility
    ProtectionRequirement.USAGE_ACCESS -> R.string.permission_action_usage
    ProtectionRequirement.NOTIFICATIONS -> R.string.permission_action_notifications
}

@StringRes
internal fun permissionSlotText(requirement: ProtectionRequirement, slot: PermissionSlot): Int =
    when (requirement) {
        ProtectionRequirement.ACCESSIBILITY_SERVICE -> when (slot) {
            PermissionSlot.WHAT_IT_ENABLES -> R.string.permission_accessibility_enables
            PermissionSlot.WHAT_IS_READ -> R.string.permission_accessibility_reads
            PermissionSlot.WHAT_IS_NEVER_READ -> R.string.permission_accessibility_never_reads
            PermissionSlot.HOW_TO_REVOKE -> R.string.permission_accessibility_revoke
        }

        ProtectionRequirement.USAGE_ACCESS -> when (slot) {
            PermissionSlot.WHAT_IT_ENABLES -> R.string.permission_usage_enables
            PermissionSlot.WHAT_IS_READ -> R.string.permission_usage_reads
            PermissionSlot.WHAT_IS_NEVER_READ -> R.string.permission_usage_never_reads
            PermissionSlot.HOW_TO_REVOKE -> R.string.permission_usage_revoke
        }

        ProtectionRequirement.NOTIFICATIONS -> when (slot) {
            PermissionSlot.WHAT_IT_ENABLES -> R.string.permission_notifications_enables
            PermissionSlot.WHAT_IS_READ -> R.string.permission_notifications_reads
            PermissionSlot.WHAT_IS_NEVER_READ -> R.string.permission_notifications_never_reads
            PermissionSlot.HOW_TO_REVOKE -> R.string.permission_notifications_revoke
        }
    }
