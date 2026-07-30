package com.blocksocial.feature.history

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.BlockEvent
import com.blocksocial.core.model.Platform
import com.blocksocial.core.model.RuleMode
import com.blocksocial.core.model.UserAction
import com.blocksocial.core.ui.theme.BlockSocialTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class HistoryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val youtube = AppRef("youtube")

    private fun event(id: String, action: UserAction) = BlockEvent(
        id = id,
        restrictedAppRef = youtube,
        occurredAt = Instant.parse("2026-07-29T09:15:00Z"),
        zone = ZoneId.of("UTC"),
        primaryReason = RuleMode.SCHEDULE,
        allReasons = listOf(RuleMode.SCHEDULE),
        userAction = action,
        bypassDurationMinutes = if (action == UserAction.BYPASSED) 5 else null,
        platform = Platform.ANDROID,
    )

    private fun show(events: List<BlockEvent>) {
        composeRule.setContent {
            BlockSocialTheme(darkTheme = true) {
                HistoryScreen(events = events) { "YouTube" }
            }
        }
    }

    @Test
    fun aBypassRowIsTheSameSizeAsAStayedRow() {
        val stayed = event("stayed", UserAction.STAYED_FOCUSED)
        val bypassed = event("bypassed", UserAction.BYPASSED)
        show(listOf(stayed, bypassed))

        val stayedHeight = composeRule.onNodeWithTag(HistoryTags.row("stayed"))
            .fetchSemanticsNode().size.height
        val bypassedHeight = composeRule.onNodeWithTag(HistoryTags.row("bypassed"))
            .fetchSemanticsNode().size.height

        assertEquals(stayedHeight, bypassedHeight)
    }

    @Test
    fun bothOutcomesAreNamedPlainlyWithoutBlame() {
        show(
            listOf(
                event("stayed", UserAction.STAYED_FOCUSED),
                event("bypassed", UserAction.BYPASSED),
            ),
        )

        composeRule.onNodeWithText("Stayed focused · Schedule").assertIsDisplayed()
        composeRule.onNodeWithText("Opened for a while · Schedule").assertIsDisplayed()
    }

    @Test
    fun onlyTheLeadingMarkDistinguishesTheTwoOutcomes() {
        show(
            listOf(
                event("stayed", UserAction.STAYED_FOCUSED),
                event("bypassed", UserAction.BYPASSED),
            ),
        )

        composeRule.onNodeWithTag(HistoryTags.mark("stayed")).assertIsDisplayed()
        composeRule.onNodeWithTag(HistoryTags.mark("bypassed")).assertIsDisplayed()
    }

    @Test
    fun anUnresolvedBlockIsListedRatherThanDropped() {
        show(listOf(event("lost", UserAction.DISMISSED_BY_SYSTEM)))

        composeRule.onNodeWithText("Closed without a decision · Schedule").assertIsDisplayed()
    }

    @Test
    fun everyRowCarriesAnAccessibilityDescription() {
        show(listOf(event("stayed", UserAction.STAYED_FOCUSED)))

        composeRule.onNodeWithContentDescription("YouTube", substring = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Stayed focused", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun anEmptyHistoryExplainsItselfWithoutJudgement() {
        show(emptyList())

        composeRule.onNodeWithTag(HistoryTags.EMPTY).assertIsDisplayed()
    }
}
