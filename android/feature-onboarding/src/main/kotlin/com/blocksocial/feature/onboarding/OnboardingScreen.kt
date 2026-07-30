package com.blocksocial.feature.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blocksocial.core.domain.ProtectionRequirement
import com.blocksocial.core.domain.RequirementStatus
import com.blocksocial.core.ui.theme.Numeric
import com.blocksocial.core.ui.theme.Spacing

object OnboardingTags {
    const val TITLE = "onboarding-title"
    const val BODY = "onboarding-body"
    const val POSITION = "onboarding-position"
    const val NEXT = "onboarding-next"
    const val BACK = "onboarding-back"
    const val SKIP = "onboarding-skip"
    const val SETUP_TITLE = "onboarding-setup-title"
    const val SETUP_DENIAL = "onboarding-setup-denial"
    const val FINISH = "onboarding-finish"
}

@Composable
fun OnboardingScreen(
    stage: OnboardingStage,
    statuses: Map<ProtectionRequirement, RequirementStatus>,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkipIntro: () -> Unit,
    onRequest: (ProtectionRequirement) -> Unit,
    onFinish: () -> Unit,
) {
    when (stage) {
        OnboardingStage.INTRO_SOFT_BLOCK,
        OnboardingStage.INTRO_IN_CONTROL,
        OnboardingStage.INTRO_ON_THIS_PHONE,
        -> IntroStep(stage = stage, onNext = onNext, onBack = onBack, onSkip = onSkipIntro)

        OnboardingStage.PERMISSIONS -> PermissionSetup(
            statuses = statuses,
            onRequest = onRequest,
            onFinish = onFinish,
        )

        OnboardingStage.ACCESSIBILITY_DISCLOSURE -> ProminentDisclosureScreen(
            onContinue = onNext,
            onNotNow = onBack,
        )

        OnboardingStage.ACCESSIBILITY_CONSENT -> AffirmativeConsentScreen(
            onAgree = { onRequest(ProtectionRequirement.ACCESSIBILITY_SERVICE) },
            onCancel = onBack,
        )
    }
}

@Composable
private fun IntroStep(
    stage: OnboardingStage,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Text(
            text = stringResource(
                R.string.onboarding_position,
                OnboardingFlow.introPosition(stage),
                OnboardingFlow.introStages.size,
            ),
            style = Numeric,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(OnboardingTags.POSITION),
        )

        Text(
            text = stringResource(introTitle(stage)),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(OnboardingTags.TITLE),
        )

        Text(
            text = stringResource(introBody(stage)),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(OnboardingTags.BODY),
        )

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .testTag(OnboardingTags.NEXT),
        ) {
            Text(stringResource(R.string.onboarding_next))
        }

        if (OnboardingFlow.back(stage) != null) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .testTag(OnboardingTags.BACK),
            ) {
                Text(stringResource(R.string.onboarding_back))
            }
        }

        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 50.dp)
                .testTag(OnboardingTags.SKIP),
        ) {
            Text(stringResource(R.string.onboarding_skip))
        }
    }
}

@Composable
private fun PermissionSetup(
    statuses: Map<ProtectionRequirement, RequirementStatus>,
    onRequest: (ProtectionRequirement) -> Unit,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Text(
            text = stringResource(R.string.onboarding_setup_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(OnboardingTags.SETUP_TITLE),
        )

        Text(
            text = stringResource(R.string.onboarding_setup_denial),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(OnboardingTags.SETUP_DENIAL),
        )

        OnboardingFlow.permissionsAskedDuringSetup.forEach { requirement ->
            PermissionCard(
                requirement = requirement,
                status = statuses[requirement] ?: RequirementStatus.NOT_ASKED,
                onRequest = onRequest,
            )
        }

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .testTag(OnboardingTags.FINISH),
        ) {
            Text(stringResource(R.string.onboarding_finish))
        }
    }
}

@StringRes
private fun introTitle(stage: OnboardingStage): Int = when (stage) {
    OnboardingStage.INTRO_SOFT_BLOCK -> R.string.onboarding_soft_block_title
    OnboardingStage.INTRO_IN_CONTROL -> R.string.onboarding_in_control_title
    OnboardingStage.INTRO_ON_THIS_PHONE -> R.string.onboarding_on_this_phone_title
    OnboardingStage.PERMISSIONS,
    OnboardingStage.ACCESSIBILITY_DISCLOSURE,
    OnboardingStage.ACCESSIBILITY_CONSENT,
    -> R.string.onboarding_setup_title
}

@StringRes
private fun introBody(stage: OnboardingStage): Int = when (stage) {
    OnboardingStage.INTRO_SOFT_BLOCK -> R.string.onboarding_soft_block_body
    OnboardingStage.INTRO_IN_CONTROL -> R.string.onboarding_in_control_body
    OnboardingStage.INTRO_ON_THIS_PHONE -> R.string.onboarding_on_this_phone_body
    OnboardingStage.PERMISSIONS,
    OnboardingStage.ACCESSIBILITY_DISCLOSURE,
    OnboardingStage.ACCESSIBILITY_CONSENT,
    -> R.string.onboarding_setup_denial
}
