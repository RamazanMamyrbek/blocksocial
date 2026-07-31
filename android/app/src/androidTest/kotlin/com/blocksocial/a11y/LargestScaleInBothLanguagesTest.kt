package com.blocksocial.a11y

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.blocksocial.block.BlockPresentation
import com.blocksocial.block.BlockScreen
import com.blocksocial.block.BlockScreenTags
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.RuleMode
import com.blocksocial.core.ui.theme.BlockSocialTheme
import com.blocksocial.feature.onboarding.OnboardingScreen
import com.blocksocial.feature.onboarding.OnboardingStage
import com.blocksocial.feature.onboarding.OnboardingTags
import com.blocksocial.shell.ScrollingScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneId
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LargestScaleInBothLanguagesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val base: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun localised(language: String): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocales(LocaleList(Locale(language)))
        return base.createConfigurationContext(configuration)
    }

    private fun show(language: String, content: @Composable () -> Unit) {
        val context = localised(language)
        composeRule.setContent {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides context.resources.configuration,
                LocalDensity provides Density(
                    density = LocalDensity.current.density,
                    fontScale = LARGEST_SCALE,
                ),
            ) {
                BlockSocialTheme(darkTheme = true) {
                    Column(modifier = Modifier.fillMaxSize()) { content() }
                }
            }
        }
    }

    private val presentation = BlockPresentation(
        app = AppRef("youtube"),
        appDisplayName = "YouTube",
        primaryReason = RuleMode.DAILY_LIMIT,
        activeUntil = null,
        zone = ZoneId.of("Europe/Berlin"),
        opensToday = 3,
        endedHereToday = 1,
        measuredMinutesToday = 38,
        limitMinutes = 5,
    )

    @Test
    fun theDecisionStaysReachableInEnglishAtTheLargestScale() {
        show("en") { BlockScreen(presentation, onStayFocused = {}, onOpenTemporarily = {}) }

        composeRule.onNodeWithTag(BlockScreenTags.HEADLINE).assertIsDisplayed()
        composeRule.onNodeWithTag(BlockScreenTags.STAY_FOCUSED).assertIsDisplayed()
        composeRule.onNodeWithTag(BlockScreenTags.OPEN_TEMPORARILY).assertIsDisplayed()
    }

    @Test
    fun theDecisionStaysReachableInRussianAtTheLargestScale() {
        show("ru") { BlockScreen(presentation, onStayFocused = {}, onOpenTemporarily = {}) }

        composeRule.onNodeWithTag(BlockScreenTags.HEADLINE).assertIsDisplayed()
        composeRule.onNodeWithTag(BlockScreenTags.STAY_FOCUSED).assertIsDisplayed()
        composeRule.onNodeWithTag(BlockScreenTags.OPEN_TEMPORARILY).assertIsDisplayed()
    }

    @Test
    fun theBlockScreenSpeaksRussianWhenTheDeviceDoes() {
        show("ru") { BlockScreen(presentation, onStayFocused = {}, onOpenTemporarily = {}) }

        composeRule.onNodeWithText("Остаться в фокусе").assertIsDisplayed()
        composeRule.onNodeWithText("Stay Focused").assertDoesNotExist()
    }

    @Test
    fun theBlockScreenSpeaksEnglishWhenTheDeviceDoes() {
        show("en") { BlockScreen(presentation, onStayFocused = {}, onOpenTemporarily = {}) }

        composeRule.onNodeWithText("Stay Focused").assertIsDisplayed()
        composeRule.onNodeWithText("Остаться в фокусе").assertDoesNotExist()
    }

    @Test
    fun theSetupScreenSurvivesRussianAtTheLargestScale() {
        show("ru") {
            ScrollingScreen {
                OnboardingScreen(
                    stage = OnboardingStage.PERMISSIONS,
                    statuses = emptyMap(),
                    onNext = {},
                    onBack = {},
                    onSkipIntro = {},
                    onRequest = {},
                    onFinish = {},
                )
            }
        }

        composeRule.onNodeWithTag(OnboardingTags.SETUP_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText("Что нужно BlockSocial").assertIsDisplayed()
    }

    @Test
    fun theIntroSurvivesRussianAtTheLargestScale() {
        show("ru") {
            ScrollingScreen {
                OnboardingScreen(
                    stage = OnboardingStage.INTRO_SOFT_BLOCK,
                    statuses = emptyMap(),
                    onNext = {},
                    onBack = {},
                    onSkipIntro = {},
                    onRequest = {},
                    onFinish = {},
                )
            }
        }

        composeRule.onNodeWithTag(OnboardingTags.TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(OnboardingTags.BODY).assertIsDisplayed()
        composeRule.onNodeWithText("Шаг 1 из 3").assertIsDisplayed()
    }

    private companion object {
        const val LARGEST_SCALE = 2.0f
    }
}
