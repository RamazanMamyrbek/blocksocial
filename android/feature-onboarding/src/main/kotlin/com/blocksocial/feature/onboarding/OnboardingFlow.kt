package com.blocksocial.feature.onboarding

import com.blocksocial.core.domain.ProtectionRequirement

enum class OnboardingStage {
    INTRO_SOFT_BLOCK,
    INTRO_IN_CONTROL,
    INTRO_ON_THIS_PHONE,
    PERMISSIONS,
    ACCESSIBILITY_DISCLOSURE,
    ACCESSIBILITY_CONSENT,
}

object OnboardingFlow {

    val start: OnboardingStage = OnboardingStage.INTRO_SOFT_BLOCK

    val introStages: List<OnboardingStage> = listOf(
        OnboardingStage.INTRO_SOFT_BLOCK,
        OnboardingStage.INTRO_IN_CONTROL,
        OnboardingStage.INTRO_ON_THIS_PHONE,
    )

    val permissionsAskedDuringSetup: List<ProtectionRequirement> = listOf(
        ProtectionRequirement.ACCESSIBILITY_SERVICE,
        ProtectionRequirement.USAGE_ACCESS,
    )

    fun next(stage: OnboardingStage): OnboardingStage? = when (stage) {
        OnboardingStage.INTRO_SOFT_BLOCK -> OnboardingStage.INTRO_IN_CONTROL
        OnboardingStage.INTRO_IN_CONTROL -> OnboardingStage.INTRO_ON_THIS_PHONE
        OnboardingStage.INTRO_ON_THIS_PHONE -> OnboardingStage.PERMISSIONS
        OnboardingStage.PERMISSIONS -> null
        OnboardingStage.ACCESSIBILITY_DISCLOSURE -> OnboardingStage.ACCESSIBILITY_CONSENT
        OnboardingStage.ACCESSIBILITY_CONSENT -> OnboardingStage.PERMISSIONS
    }

    fun back(stage: OnboardingStage): OnboardingStage? = when (stage) {
        OnboardingStage.INTRO_SOFT_BLOCK -> null
        OnboardingStage.INTRO_IN_CONTROL -> OnboardingStage.INTRO_SOFT_BLOCK
        OnboardingStage.INTRO_ON_THIS_PHONE -> OnboardingStage.INTRO_IN_CONTROL
        OnboardingStage.PERMISSIONS -> null
        OnboardingStage.ACCESSIBILITY_DISCLOSURE -> OnboardingStage.PERMISSIONS
        OnboardingStage.ACCESSIBILITY_CONSENT -> OnboardingStage.ACCESSIBILITY_DISCLOSURE
    }

    fun skipIntro(): OnboardingStage = OnboardingStage.PERMISSIONS

    fun request(requirement: ProtectionRequirement): OnboardingStage? = when (requirement) {
        ProtectionRequirement.ACCESSIBILITY_SERVICE -> OnboardingStage.ACCESSIBILITY_DISCLOSURE
        ProtectionRequirement.USAGE_ACCESS -> null
        ProtectionRequirement.NOTIFICATIONS -> null
    }

    fun introPosition(stage: OnboardingStage): Int = introStages.indexOf(stage) + 1
}
