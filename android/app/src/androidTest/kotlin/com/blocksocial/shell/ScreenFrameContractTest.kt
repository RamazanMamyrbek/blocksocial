package com.blocksocial.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.blocksocial.core.domain.ProtectionBanner
import com.blocksocial.core.domain.ProtectionRequirement
import com.blocksocial.core.domain.RequirementHealth
import com.blocksocial.core.domain.RequirementStatus
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.BlockEvent
import com.blocksocial.core.model.Platform
import com.blocksocial.core.model.RestrictionRule
import com.blocksocial.core.model.RuleMode
import com.blocksocial.core.model.UserAction
import com.blocksocial.core.ui.theme.BlockSocialTheme
import com.blocksocial.feature.appselection.AppSelectionRow
import com.blocksocial.feature.appselection.AppSelectionScreen
import com.blocksocial.feature.appselection.InstallState
import com.blocksocial.feature.dashboard.DashboardScreen
import com.blocksocial.feature.dashboard.DashboardState
import com.blocksocial.feature.history.HistoryScreen
import com.blocksocial.feature.onboarding.ProtectionHealthScreen
import com.blocksocial.feature.rules.RuleDraft
import com.blocksocial.feature.rules.RuleEditorScreen
import com.blocksocial.feature.rules.RuleListScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class ScreenFrameContractTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun inShell(content: @Composable () -> Unit) {
        composeRule.setContent {
            BlockSocialTheme(darkTheme = true) {
                Column(modifier = Modifier.fillMaxSize()) {
                    BlockSocialShell(navigator = ShellNavigator()) { content() }
                }
            }
        }
        composeRule.onNodeWithTag(ShellTags.screen(Destination.Dashboard)).assertExists()
    }

    @Test
    fun theDashboardFitsItsFrame() {
        inShell { ScrollingScreen { DashboardScreen(state = busyDay) } }
    }

    @Test
    fun protectionHealthFitsItsFrame() {
        inShell {
            ScrollingScreen {
                ProtectionHealthScreen(
                    items = ProtectionRequirement.entries.map {
                        RequirementHealth(it, RequirementStatus.DENIED)
                    },
                    blockingRuns = false,
                    onRepair = {},
                )
            }
        }
    }

    @Test
    fun theRuleEditorFitsItsFrameUnderTheRuleList() {
        inShell {
            ScrollingScreen {
                RuleListScreen(
                    rules = listOf(RestrictionRule.AlwaysOn(id = "one", enabled = true)),
                    onEnabledChange = { _, _ -> },
                    onEdit = {},
                )
                RuleEditorScreen(
                    draft = RuleDraft(id = "two", mode = RuleMode.SCHEDULE),
                    onDraftChange = {},
                    onSave = {},
                )
            }
        }
    }

    @Test
    fun theHistoryListFitsItsFrame() {
        inShell {
            ScrollsItselfScreen {
                HistoryScreen(events = listOf(event), displayNameOf = { "YouTube" })
            }
        }
    }

    @Test
    fun theApplicationListFitsItsFrame() {
        inShell {
            AppSelectionScreen(
                rows = listOf(
                    AppSelectionRow(
                        ref = APP,
                        displayName = "YouTube",
                        installState = InstallState.INSTALLED,
                        selected = true,
                    ),
                ),
                onSelectedChange = { _, _ -> },
            )
        }
    }

    private val busyDay = DashboardState(
        protection = ProtectionBanner.STOPPED,
        interventionsToday = 6,
        stayedFocusedToday = 4,
        bypassedToday = 2,
        refusalRateLastSevenDays = 0.66,
        activeRules = 3,
        pausedRules = 1,
        streakDays = 5,
        bypassAllowancePerDay = 2,
    )

    private val event = BlockEvent(
        id = "one",
        restrictedAppRef = APP,
        occurredAt = Instant.parse("2026-07-30T12:00:00Z"),
        zone = ZoneId.of("Europe/Berlin"),
        primaryReason = RuleMode.ALWAYS,
        allReasons = listOf(RuleMode.ALWAYS),
        userAction = UserAction.STAYED_FOCUSED,
        bypassDurationMinutes = null,
        platform = Platform.ANDROID,
    )

    private companion object {
        val APP = AppRef("youtube")
    }
}
