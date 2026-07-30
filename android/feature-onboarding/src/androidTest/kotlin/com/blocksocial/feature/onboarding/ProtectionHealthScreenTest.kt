package com.blocksocial.feature.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.blocksocial.core.domain.ProtectionRequirement
import com.blocksocial.core.domain.RequirementHealth
import com.blocksocial.core.domain.RequirementStatus
import com.blocksocial.core.ui.theme.BlockSocialTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProtectionHealthScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun items(accessibility: RequirementStatus, usage: RequirementStatus = RequirementStatus.HEALTHY) =
        listOf(
            RequirementHealth(ProtectionRequirement.ACCESSIBILITY_SERVICE, accessibility),
            RequirementHealth(ProtectionRequirement.USAGE_ACCESS, usage),
            RequirementHealth(ProtectionRequirement.NOTIFICATIONS, RequirementStatus.HEALTHY),
        )

    private fun show(
        accessibility: RequirementStatus,
        usage: RequirementStatus = RequirementStatus.HEALTHY,
        onRepair: (ProtectionRequirement) -> Unit = {},
    ) {
        val shown = items(accessibility, usage)
        composeRule.setContent {
            BlockSocialTheme(darkTheme = true) {
                ProtectionHealthScreen(
                    items = shown,
                    blockingRuns = shown.none { it.blocksProtection },
                    onRepair = onRepair,
                )
            }
        }
    }

    @Test
    fun everythingWorkingSaysBlockingIsRunning() {
        show(RequirementStatus.HEALTHY)

        composeRule.onNodeWithText("Blocking is running.").assertIsDisplayed()
    }

    @Test
    fun aForceStoppedServiceIsReportedAsNotRunningRatherThanHealthy() {
        show(RequirementStatus.ENABLED_BUT_NOT_RUNNING)

        composeRule.onNodeWithText("Turned on in settings, but not running").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Android shows this as on, but the service is not running. This usually happens " +
                "after the app is force-stopped. Rules stay saved; blocking does not run until " +
                "you turn it off and on again.",
        ).assertIsDisplayed()
    }

    @Test
    fun aSilentServiceIsReportedBrokenAndSaysWhatSurvives() {
        show(RequirementStatus.RUNNING_BUT_SILENT)

        composeRule.onNodeWithText("Turned on, but delivering nothing").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Blocking is not running. Your rules are saved and unchanged, but nothing is " +
                "being paused right now.",
        ).assertIsDisplayed()
    }

    @Test
    fun losingUsageAccessSaysSchedulesKeepWorking() {
        show(RequirementStatus.HEALTHY, usage = RequirementStatus.DENIED)

        composeRule.onNodeWithText(
            "Daily limits cannot be measured without this. Schedules and always-on rules " +
                "keep working exactly as before.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Blocking is running.").assertIsDisplayed()
    }

    @Test
    fun everyDegradedItemOffersOneRepairAction() {
        val repaired = mutableListOf<ProtectionRequirement>()
        show(RequirementStatus.DENIED, onRepair = { repaired += it })

        composeRule.onNodeWithTag(
            ProtectionHealthTags.repair(ProtectionRequirement.ACCESSIBILITY_SERVICE),
        ).performClick()

        assertEquals(listOf(ProtectionRequirement.ACCESSIBILITY_SERVICE), repaired)
    }

    @Test
    fun aHealthyItemOffersNoRepairBecauseThereIsNothingToFix() {
        show(RequirementStatus.HEALTHY)

        composeRule.onNodeWithTag(
            ProtectionHealthTags.repair(ProtectionRequirement.ACCESSIBILITY_SERVICE),
        ).assertDoesNotExist()
    }

    @Test
    fun everyItemCarriesAShapeSoGreyscaleStillParses() {
        show(RequirementStatus.ENABLED_BUT_NOT_RUNNING, usage = RequirementStatus.DENIED)

        listOf(
            ProtectionRequirement.ACCESSIBILITY_SERVICE,
            ProtectionRequirement.USAGE_ACCESS,
            ProtectionRequirement.NOTIFICATIONS,
        ).forEach { requirement ->
            composeRule.onNodeWithTag(ProtectionHealthTags.mark(requirement)).assertIsDisplayed()
        }
    }

    @Test
    fun everyItemCarriesAnAccessibilityDescriptionNamingItsStatus() {
        show(RequirementStatus.ENABLED_BUT_NOT_RUNNING)

        composeRule.onNodeWithContentDescription(
            "Accessibility access, Turned on in settings, but not running",
        ).assertIsDisplayed()
    }

    @Test
    fun theOemGuidanceSaysItIsFromVendorDocumentationRatherThanObserved() {
        show(RequirementStatus.HEALTHY)

        composeRule.onNodeWithTag(ProtectionHealthTags.OEM).assertIsDisplayed()
        composeRule.onNodeWithText(
            "Some manufacturers stop background services to save battery. If that happens, " +
                "allow BlockSocial to run in the background in your phone settings. This " +
                "guidance is written from vendor documentation and has not been observed on a " +
                "device by us.",
        ).assertIsDisplayed()
    }
}
