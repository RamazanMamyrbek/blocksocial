package com.blocksocial.feature.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.blocksocial.core.domain.ProtectionRequirement
import com.blocksocial.core.domain.RequirementStatus
import com.blocksocial.core.ui.theme.BlockSocialTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        stage: OnboardingStage,
        statuses: Map<ProtectionRequirement, RequirementStatus> = emptyMap(),
        onNext: () -> Unit = {},
        onBack: () -> Unit = {},
        onSkipIntro: () -> Unit = {},
        onRequest: (ProtectionRequirement) -> Unit = {},
        onFinish: () -> Unit = {},
    ) {
        composeRule.setContent {
            BlockSocialTheme(darkTheme = true) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OnboardingScreen(
                        stage = stage,
                        statuses = statuses,
                        onNext = onNext,
                        onBack = onBack,
                        onSkipIntro = onSkipIntro,
                        onRequest = onRequest,
                        onFinish = onFinish,
                    )
                }
            }
        }
    }

    @Test
    fun theFirstIntroStepShowsItsPositionAndNoWayBack() {
        show(OnboardingStage.INTRO_SOFT_BLOCK)

        composeRule.onNodeWithTag(OnboardingTags.POSITION).assertIsDisplayed()
        composeRule.onNodeWithTag(OnboardingTags.TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(OnboardingTags.BODY).assertIsDisplayed()
        composeRule.onNodeWithTag(OnboardingTags.BACK).assertDoesNotExist()
    }

    @Test
    fun everyIntroStepCanBeSkipped() {
        var skipped = 0
        show(OnboardingStage.INTRO_IN_CONTROL, onSkipIntro = { skipped++ })

        composeRule.onNodeWithTag(OnboardingTags.SKIP).performScrollTo().performClick()

        assertEquals(1, skipped)
    }

    @Test
    fun theSetupScreenAsksForBlockingAndDailyLimitsOnly() {
        show(OnboardingStage.PERMISSIONS)

        composeRule
            .onNodeWithTag(PermissionCardTags.card(ProtectionRequirement.ACCESSIBILITY_SERVICE))
            .assertExists()
        composeRule
            .onNodeWithTag(PermissionCardTags.card(ProtectionRequirement.USAGE_ACCESS))
            .assertExists()
        composeRule
            .onNodeWithTag(PermissionCardTags.card(ProtectionRequirement.NOTIFICATIONS))
            .assertDoesNotExist()
    }

    @Test
    fun everyCardShowsAllFourSlots() {
        show(OnboardingStage.PERMISSIONS)

        OnboardingFlow.permissionsAskedDuringSetup.forEach { requirement ->
            PermissionSlot.entries.forEach { slot ->
                composeRule.onNodeWithTag(PermissionCardTags.slot(requirement, slot)).assertExists()
            }
        }
    }

    @Test
    fun aCardThatLeadsToASystemScreenSaysSo() {
        show(OnboardingStage.PERMISSIONS)

        composeRule
            .onNodeWithTag(PermissionCardTags.footnote(ProtectionRequirement.ACCESSIBILITY_SERVICE))
            .assertExists()
    }

    @Test
    fun aWorkingPermissionOffersNoButton() {
        show(
            OnboardingStage.PERMISSIONS,
            statuses = mapOf(ProtectionRequirement.USAGE_ACCESS to RequirementStatus.HEALTHY),
        )

        composeRule
            .onNodeWithTag(PermissionCardTags.action(ProtectionRequirement.USAGE_ACCESS))
            .assertDoesNotExist()
        composeRule
            .onNodeWithTag(PermissionCardTags.action(ProtectionRequirement.ACCESSIBILITY_SERVICE))
            .assertExists()
    }

    @Test
    fun theSetupScreenStatesThatDenialIsSurvivable() {
        show(OnboardingStage.PERMISSIONS)

        composeRule.onNodeWithTag(OnboardingTags.SETUP_DENIAL).assertIsDisplayed()
        composeRule.onNodeWithTag(OnboardingTags.FINISH).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun usageAccessIsRequestedWithoutPassingThroughTheAccessibilityDisclosure() {
        val requested = mutableListOf<ProtectionRequirement>()
        show(OnboardingStage.PERMISSIONS, onRequest = { requested += it })

        composeRule
            .onNodeWithTag(PermissionCardTags.action(ProtectionRequirement.USAGE_ACCESS))
            .performScrollTo()
            .performClick()

        assertEquals(listOf(ProtectionRequirement.USAGE_ACCESS), requested)
    }

    @Test
    fun theDisclosureStageLeadsToConsentRatherThanStraightToTheSystem() {
        var advanced = 0
        val requested = mutableListOf<ProtectionRequirement>()
        show(OnboardingStage.ACCESSIBILITY_DISCLOSURE, onNext = { advanced++ }, onRequest = { requested += it })

        composeRule.onNodeWithTag(DisclosureTags.CONTINUE).performScrollTo().performClick()

        assertEquals(1, advanced)
        assertEquals(emptyList<ProtectionRequirement>(), requested)
    }

    @Test
    fun onlyTheConsentStageHandsOverToTheSystem() {
        val requested = mutableListOf<ProtectionRequirement>()
        show(OnboardingStage.ACCESSIBILITY_CONSENT, onRequest = { requested += it })

        composeRule.onNodeWithTag(DisclosureTags.AGREE).performScrollTo().performClick()

        assertEquals(listOf(ProtectionRequirement.ACCESSIBILITY_SERVICE), requested)
    }
}
