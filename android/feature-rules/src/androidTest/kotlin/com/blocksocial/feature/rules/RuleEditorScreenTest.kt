package com.blocksocial.feature.rules

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.blocksocial.core.model.RuleMode
import com.blocksocial.core.ui.theme.BlockSocialTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class RuleEditorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(initial: RuleDraft, onSave: () -> Unit = {}) {
        composeRule.setContent {
            var draft by remember { mutableStateOf(initial) }
            BlockSocialTheme(darkTheme = true) {
                RuleEditorScreen(draft = draft, onDraftChange = { draft = it }, onSave = onSave)
            }
        }
    }

    private fun overnight() = RuleDraft(
        id = "r1",
        mode = RuleMode.SCHEDULE,
        daysOfWeek = setOf(DayOfWeek.MONDAY),
        startLocalTime = LocalTime.of(22, 0),
        endLocalTime = LocalTime.of(7, 0),
    )

    @Test
    fun anOvernightIntervalIsStatedThreeWays() {
        show(overnight())

        composeRule.onNodeWithTag(RuleEditorTags.OVERNIGHT_WORD).assertIsDisplayed()
        composeRule.onNodeWithTag(RuleEditorTags.INTERVAL_BAR).assertIsDisplayed()
        composeRule.onNodeWithText("22:00 tonight until 07:00 the next morning").assertIsDisplayed()
    }

    @Test
    fun aSameDayIntervalSaysSoInsteadOfSayingOvernight() {
        show(overnight().copy(startLocalTime = LocalTime.of(18, 0), endLocalTime = LocalTime.of(21, 0)))

        composeRule.onNodeWithTag(RuleEditorTags.OVERNIGHT_WORD).assertDoesNotExist()
        composeRule.onNodeWithText("18:00 until 21:00 the same day").assertIsDisplayed()
    }

    @Test
    fun onlyTheFieldsTheModeUsesAreShown() {
        show(RuleDraft(id = "r1", mode = RuleMode.ALWAYS))

        composeRule.onNodeWithTag(RuleEditorTags.START).assertDoesNotExist()
        composeRule.onNodeWithTag(RuleEditorTags.day(DayOfWeek.MONDAY)).assertDoesNotExist()

        composeRule.onNodeWithTag(RuleEditorTags.mode(RuleMode.SCHEDULE)).performClick()

        composeRule.onNodeWithTag(RuleEditorTags.START).assertIsDisplayed()
        composeRule.onNodeWithTag(RuleEditorTags.day(DayOfWeek.MONDAY)).assertIsDisplayed()
    }

    @Test
    fun aScheduleWithNoDaysExplainsHowToFixItAndCannotBeSaved() {
        show(overnight().copy(daysOfWeek = emptySet()))

        composeRule.onNodeWithText("Pick at least one day. A schedule with no days never applies.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag(RuleEditorTags.SAVE).assertIsNotEnabled()
    }

    @Test
    fun anEmptyIntervalNamesTheOffendingTime() {
        show(overnight().copy(startLocalTime = LocalTime.of(22, 0), endLocalTime = LocalTime.of(22, 0)))

        composeRule.onNodeWithText("Start and end are both 22:00. Move the end time so the interval has a length.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag(RuleEditorTags.SAVE).assertIsNotEnabled()
    }

    @Test
    fun choosingADayClearsTheProblemAndEnablesSaving() {
        show(overnight().copy(daysOfWeek = emptySet()))

        composeRule.onNodeWithTag(RuleEditorTags.SAVE).assertIsNotEnabled()
        composeRule.onNodeWithTag(RuleEditorTags.day(DayOfWeek.WEDNESDAY)).performClick()

        composeRule.onNodeWithTag(RuleEditorTags.PROBLEM).assertDoesNotExist()
        composeRule.onNodeWithTag(RuleEditorTags.SAVE).assertIsEnabled()
    }

    @Test
    fun aDailyLimitOutOfRangeStatesTheAllowedRange() {
        show(RuleDraft(id = "r1", mode = RuleMode.DAILY_LIMIT, dailyLimitMinutes = 1))

        composeRule.onNodeWithText("Set a limit between 5 and 720 minutes.").assertIsDisplayed()
        composeRule.onNodeWithTag(RuleEditorTags.SAVE).assertIsNotEnabled()
    }
}
