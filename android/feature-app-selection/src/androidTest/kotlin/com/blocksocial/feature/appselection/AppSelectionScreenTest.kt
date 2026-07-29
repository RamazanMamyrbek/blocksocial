package com.blocksocial.feature.appselection

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.ui.theme.BlockSocialTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSelectionScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val instagram = AppRef("instagram")
    private val tiktok = AppRef("tiktok")
    private val vk = AppRef("vk")

    private val rows = listOf(
        AppSelectionRow(instagram, "Instagram", InstallState.INSTALLED, selected = true),
        AppSelectionRow(tiktok, "TikTok", InstallState.INSTALLED, selected = false),
        AppSelectionRow(vk, "VK", InstallState.NOT_INSTALLED, selected = false),
    )

    private fun show(
        shown: List<AppSelectionRow> = rows,
        onSelectedChange: (AppRef, Boolean) -> Unit = { _, _ -> },
    ) {
        composeRule.setContent {
            BlockSocialTheme(darkTheme = true) {
                AppSelectionScreen(rows = shown, onSelectedChange = onSelectedChange)
            }
        }
    }

    @Test
    fun aSelectedInstalledApplicationShowsItsToggleOn() {
        show()

        composeRule.onNodeWithTag(AppSelectionTags.toggle(instagram)).assertIsOn()
    }

    @Test
    fun anUnselectedInstalledApplicationShowsItsToggleOff() {
        show()

        composeRule.onNodeWithTag(AppSelectionTags.toggle(tiktok)).assertIsOff()
    }

    @Test
    fun aNotInstalledApplicationHasNoToggleAndIsNotAnError() {
        show()

        composeRule.onNodeWithTag(AppSelectionTags.row(vk)).assertIsDisplayed()
        composeRule.onNodeWithTag(AppSelectionTags.toggle(vk)).assertDoesNotExist()
        composeRule.onNodeWithText("Not installed on this phone").assertIsDisplayed()
    }

    @Test
    fun everyRowCarriesAnAccessibilityDescriptionNamingItsState() {
        show()

        composeRule.onNodeWithContentDescription("Instagram, selected").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("TikTok, not selected").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("VK, Not installed on this phone").assertIsDisplayed()
    }

    @Test
    fun togglingAnApplicationReportsTheChange() {
        val changes = mutableListOf<Pair<AppRef, Boolean>>()
        show(onSelectedChange = { ref, selected -> changes += ref to selected })

        composeRule.onNodeWithTag(AppSelectionTags.toggle(tiktok)).performClick()

        assertEquals(listOf(tiktok to true), changes)
    }

    @Test
    fun theScreenExplainsThatTheListIsSupportedApplicationsRatherThanAllApplications() {
        show()

        composeRule.onNodeWithText(
            "BlockSocial works with the supported applications below. " +
                "It cannot see the other applications on your phone, " +
                "and this list will not grow on its own.",
        ).assertIsDisplayed()
    }

    @Test
    fun anEmptyResultExplainsItselfRatherThanShowingADeadEnd() {
        show(shown = listOf(AppSelectionRow(vk, "VK", InstallState.NOT_INSTALLED, selected = false)))

        composeRule.onNodeWithTag(AppSelectionTags.EMPTY).assertIsDisplayed()
    }
}
