package com.blocksocial.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.blocksocial.core.ui.theme.BlockSocialTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RotationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val restoration = StateRestorationTester(composeRule)

    private fun showShell() {
        restoration.setContent {
            BlockSocialTheme(darkTheme = true) {
                Column(modifier = Modifier.fillMaxSize()) {
                    BlockSocialShell(navigator = rememberShellNavigator()) { destination ->
                        Text(text = destination.encode())
                    }
                }
            }
        }
    }

    @Test
    fun theSelectedTabSurvivesRotation() {
        showShell()

        composeRule.onNodeWithTag(ShellTags.tab(Destination.History)).performClick()
        composeRule.onNodeWithTag(ShellTags.screen(Destination.History)).assertExists()

        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithTag(ShellTags.screen(Destination.History)).assertExists()
    }

    @Test
    fun theDashboardIsStillTheDashboardAfterRotation() {
        showShell()

        composeRule.onNodeWithTag(ShellTags.screen(Destination.Home)).assertExists()

        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithTag(ShellTags.screen(Destination.Home)).assertExists()
        composeRule.onNodeWithTag(ShellTags.BACK).assertDoesNotExist()
    }

    @Test
    fun switchingTabsAndRotatingLandsOnTheTabTheUserChose() {
        showShell()

        composeRule.onNodeWithTag(ShellTags.tab(Destination.Today)).performClick()
        restoration.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithTag(ShellTags.screen(Destination.Today)).assertExists()

        composeRule.onNodeWithTag(ShellTags.tab(Destination.Home)).performClick()
        restoration.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithTag(ShellTags.screen(Destination.Home)).assertExists()
    }
}
