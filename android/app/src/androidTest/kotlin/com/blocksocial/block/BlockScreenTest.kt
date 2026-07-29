package com.blocksocial.block

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.RuleMode
import com.blocksocial.core.ui.theme.BlockSocialTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class BlockScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun presentation(
        reason: RuleMode = RuleMode.SCHEDULE,
        activeUntil: Instant? = Instant.parse("2026-07-27T12:00:00Z"),
    ) = BlockPresentation(
        app = AppRef("instagram"),
        appDisplayName = "Instagram",
        primaryReason = reason,
        activeUntil = activeUntil,
        zone = ZoneId.of("UTC"),
        opensToday = 6,
        endedHereToday = 4,
    )

    private fun show(
        darkTheme: Boolean = true,
        fontScale: Float = 1f,
        reason: RuleMode = RuleMode.SCHEDULE,
        onStayFocused: () -> Unit = {},
        onOpenTemporarily: () -> Unit = {},
    ) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale),
            ) {
                BlockSocialTheme(darkTheme = darkTheme) {
                    BlockScreen(
                        presentation = presentation(reason = reason),
                        onStayFocused = onStayFocused,
                        onOpenTemporarily = onOpenTemporarily,
                    )
                }
            }
        }
    }

    @Test
    fun theWholeDecisionIsOnScreenInDarkTheme() {
        show(darkTheme = true)

        composeRule.onNodeWithTag(BlockScreenTags.HEADLINE).assertIsDisplayed()
        composeRule.onNodeWithTag(BlockScreenTags.STAY_FOCUSED).assertIsDisplayed()
        composeRule.onNodeWithTag(BlockScreenTags.OPEN_TEMPORARILY).assertIsDisplayed()
        composeRule.onNodeWithTag(BlockScreenTags.FOOTNOTE).assertIsDisplayed()
    }

    @Test
    fun theWholeDecisionIsOnScreenInLightTheme() {
        show(darkTheme = false)

        composeRule.onNodeWithTag(BlockScreenTags.HEADLINE).assertIsDisplayed()
        composeRule.onNodeWithTag(BlockScreenTags.STAY_FOCUSED).assertIsDisplayed()
        composeRule.onNodeWithTag(BlockScreenTags.OPEN_TEMPORARILY).assertIsDisplayed()
    }

    @Test
    fun bothActionsSurviveTheLargestFontScale() {
        show(fontScale = 2f)

        composeRule.onNodeWithTag(BlockScreenTags.HEADLINE).assertIsDisplayed()
        composeRule.onNodeWithTag(BlockScreenTags.STAY_FOCUSED).assertIsDisplayed()
        composeRule.onNodeWithTag(BlockScreenTags.OPEN_TEMPORARILY).assertIsDisplayed()
    }

    @Test
    fun everyInteractiveElementIsLabelledAndClickable() {
        show()

        composeRule.onNodeWithText("Stay Focused").assertHasClickAction()
        composeRule.onNodeWithText("Open Temporarily").assertHasClickAction()
    }

    @Test
    fun theScreenNamesTheApplicationAndTheRecord() {
        show()

        composeRule.onNodeWithText("You opened Instagram").assertIsDisplayed()
        composeRule.onNodeWithText("4 of 6 opens today ended here").assertIsDisplayed()
    }

    @Test
    fun aScheduleNamesWhenItEnds() {
        show(reason = RuleMode.SCHEDULE)

        composeRule.onNodeWithText("A schedule is holding this until 12:00").assertIsDisplayed()
    }

    @Test
    fun anAlwaysOnRuleDoesNotPromiseAnEndTime() {
        show(reason = RuleMode.ALWAYS)

        composeRule.onNodeWithText("You set this app to stay closed").assertIsDisplayed()
    }

    @Test
    fun theActionsReportTheUsersChoice() {
        var stayed = 0
        var opened = 0
        show(onStayFocused = { stayed++ }, onOpenTemporarily = { opened++ })

        composeRule.onNodeWithTag(BlockScreenTags.STAY_FOCUSED).performClick()
        composeRule.onNodeWithTag(BlockScreenTags.OPEN_TEMPORARILY).performClick()

        assertEquals(1, stayed)
        assertEquals(1, opened)
    }

    @Test
    fun thePrimaryActionComesBeforeTheSecondaryAndTheFootnote() {
        show()

        val primary = composeRule.onNodeWithTag(BlockScreenTags.STAY_FOCUSED)
            .fetchSemanticsNode().positionInRoot.y
        val secondary = composeRule.onNodeWithTag(BlockScreenTags.OPEN_TEMPORARILY)
            .fetchSemanticsNode().positionInRoot.y
        val footnote = composeRule.onNodeWithTag(BlockScreenTags.FOOTNOTE)
            .fetchSemanticsNode().positionInRoot.y

        assertEquals(true, primary < secondary)
        assertEquals(true, secondary < footnote)
    }
}
