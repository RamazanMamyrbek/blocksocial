package com.blocksocial.feature.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.blocksocial.core.ui.theme.BlockSocialTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(state: DashboardState) {
        composeRule.setContent {
            BlockSocialTheme(darkTheme = true) { DashboardScreen(state) }
        }
    }

    private val busyDay = DashboardState(
        interventionsToday = 6,
        stayedFocusedToday = 4,
        bypassedToday = 2,
        refusalRateLastSevenDays = 0.6666,
        activeRules = 3,
        pausedRules = 1,
        streakDays = 5,
        bypassAllowancePerDay = 2,
    )

    @Test
    fun measuredValuesArePlain() {
        show(busyDay)

        composeRule.onNodeWithText("4 of 6 opens").assertIsDisplayed()
        composeRule.onNodeWithText("67% of opens, last 7 days").assertIsDisplayed()
        composeRule.onNodeWithText("3 active, 1 paused").assertIsDisplayed()
    }

    @Test
    fun theStreakExplainsItsOwnRuleBeforeItCanBeLost() {
        show(busyDay)

        composeRule.onNodeWithText("5 days").assertIsDisplayed()
        composeRule.onNodeWithText("A day counts when you open no more than 2 times on purpose.")
            .assertIsDisplayed()
    }

    @Test
    fun aOneDayStreakIsNotWrittenAsOneDays() {
        show(busyDay.copy(streakDays = 1))

        composeRule.onNodeWithText("1 day").assertIsDisplayed()
        composeRule.onNodeWithText("1 days").assertDoesNotExist()
    }

    @Test
    fun anUnmeasurableMetricIsNamedAndExplainedRatherThanHidden() {
        show(busyDay)

        composeRule.onNodeWithTag(DashboardTags.HOURS_SAVED).assertIsDisplayed()
        composeRule.onNodeWithText(
            "Hours saved is not shown. BlockSocial cannot measure it honestly, " +
                "so it is absent rather than reported as zero.",
        ).assertIsDisplayed()
    }

    @Test
    fun aRateWithNothingToDivideSaysSoInsteadOfShowingZero() {
        show(busyDay.copy(refusalRateLastSevenDays = null))

        composeRule.onNodeWithText("Not enough decisions yet").assertIsDisplayed()
        composeRule.onNodeWithText("0% of opens, last 7 days").assertDoesNotExist()
    }

    @Test
    fun anEmptyDaySaysNumbersWillAppearRatherThanShowingABareZero() {
        show(DashboardState.Empty)

        composeRule.onNodeWithTag(DashboardTags.EMPTY).assertIsDisplayed()
    }





}
