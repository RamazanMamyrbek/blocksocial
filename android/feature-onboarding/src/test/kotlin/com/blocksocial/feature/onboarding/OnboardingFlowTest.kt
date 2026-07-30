package com.blocksocial.feature.onboarding

import com.blocksocial.core.domain.ProtectionRequirement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingFlowTest {

    @Test
    fun theIntroIsWalkedInOrderAndEndsAtTheSetupScreen() {
        val walked = generateSequence(OnboardingFlow.start, OnboardingFlow::next).toList()

        assertEquals(OnboardingFlow.introStages + OnboardingStage.PERMISSIONS, walked)
    }

    @Test
    fun theIntroIsShortEnoughToRead() {
        assertTrue("the intro must stay a few steps", OnboardingFlow.introStages.size <= 4)
    }

    @Test
    fun everyIntroStepKnowsItsPosition() {
        OnboardingFlow.introStages.forEachIndexed { index, stage ->
            assertEquals(index + 1, OnboardingFlow.introPosition(stage))
        }
    }

    @Test
    fun theIntroCanBeSkipped() {
        assertEquals(OnboardingStage.PERMISSIONS, OnboardingFlow.skipIntro())
    }

    @Test
    fun consentIsOnlyEverReachedThroughTheDisclosure() {
        val stagesLeadingToConsent = OnboardingStage.entries
            .filter { OnboardingFlow.next(it) == OnboardingStage.ACCESSIBILITY_CONSENT }

        assertEquals(listOf(OnboardingStage.ACCESSIBILITY_DISCLOSURE), stagesLeadingToConsent)
        assertEquals(
            OnboardingStage.ACCESSIBILITY_DISCLOSURE,
            OnboardingFlow.request(ProtectionRequirement.ACCESSIBILITY_SERVICE),
        )
    }

    @Test
    fun cancellingConsentReturnsToTheDisclosureAndThenToTheCards() {
        assertEquals(
            OnboardingStage.ACCESSIBILITY_DISCLOSURE,
            OnboardingFlow.back(OnboardingStage.ACCESSIBILITY_CONSENT),
        )
        assertEquals(
            OnboardingStage.PERMISSIONS,
            OnboardingFlow.back(OnboardingStage.ACCESSIBILITY_DISCLOSURE),
        )
    }

    @Test
    fun agreeingReturnsToTheCardsRatherThanEndingTheFlow() {
        assertEquals(
            OnboardingStage.PERMISSIONS,
            OnboardingFlow.next(OnboardingStage.ACCESSIBILITY_CONSENT),
        )
    }

    @Test
    fun theIntroIsNotReenteredFromTheSetupScreen() {
        assertNull(OnboardingFlow.back(OnboardingStage.PERMISSIONS))
        assertNull(OnboardingFlow.back(OnboardingStage.INTRO_SOFT_BLOCK))
    }

    @Test
    fun usageAccessIsAskedForOnItsOwnWithoutTheAccessibilityDisclosure() {
        assertNull(OnboardingFlow.request(ProtectionRequirement.USAGE_ACCESS))
        assertTrue(
            OnboardingFlow.permissionsAskedDuringSetup
                .contains(ProtectionRequirement.USAGE_ACCESS),
        )
    }

    @Test
    fun notificationsAreNeverAskedForDuringSetup() {
        assertTrue(
            "notifications are requested only when first needed",
            !OnboardingFlow.permissionsAskedDuringSetup
                .contains(ProtectionRequirement.NOTIFICATIONS),
        )
    }
}
