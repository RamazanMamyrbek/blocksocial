package com.blocksocial.debug

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TokenPreviewTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<TokenPreviewActivity>()

    @Test
    fun everyColorRoleIsShown() {
        listOf(
            "surface",
            "surfaceContainer",
            "surfaceContainerHigh",
            "onSurface",
            "onSurfaceVariant",
            "outline",
            "primary",
            "onPrimary",
            "success",
            "warning",
            "danger",
        ).forEach { role ->
            composeRule.onNodeWithText(role).assertExists("color role $role is missing")
        }
    }

    @Test
    fun everyScaleSectionIsShown() {
        listOf("Color", "Type", "Spacing", "Radius", "Motion").forEach { section ->
            composeRule.onNodeWithText(section).assertExists("section $section is missing")
        }
    }

    @Test
    fun theMotionSampleIsInteractive() {
        composeRule.onNodeWithText("Animate").assertExists()
        composeRule.onNodeWithText("Animate").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Animate").assertExists()
    }
}
