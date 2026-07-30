package com.blocksocial

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.blocksocial.core.data.preferences.PreferencesRepository
import com.blocksocial.core.domain.NotificationAsk
import com.blocksocial.core.domain.ProtectionRequirement
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.RuleMode
import com.blocksocial.core.ui.theme.BlockSocialTheme
import com.blocksocial.core.ui.theme.Spacing
import com.blocksocial.feature.appselection.AppSelectionController
import com.blocksocial.feature.appselection.AppSelectionScreen
import com.blocksocial.feature.dashboard.DashboardController
import com.blocksocial.feature.dashboard.DashboardScreen
import com.blocksocial.feature.dashboard.DashboardState
import com.blocksocial.feature.history.HistoryController
import com.blocksocial.feature.history.HistoryPage
import com.blocksocial.feature.history.HistoryScreen
import com.blocksocial.feature.onboarding.OnboardingFlow
import com.blocksocial.feature.onboarding.OnboardingScreen
import com.blocksocial.feature.onboarding.OnboardingStage
import com.blocksocial.feature.onboarding.ProtectionHealthScreen
import com.blocksocial.feature.rules.RuleDraft
import com.blocksocial.feature.rules.RuleEditorScreen
import com.blocksocial.feature.rules.RuleListScreen
import com.blocksocial.feature.rules.RulesController
import com.blocksocial.health.ProtectionHealth
import com.blocksocial.health.ProtectionHealthProbe
import com.blocksocial.shell.BlockSocialShell
import com.blocksocial.shell.Destination
import com.blocksocial.shell.ScrollingScreen
import com.blocksocial.shell.ScrollsItselfScreen
import com.blocksocial.shell.ShellNavigator
import com.blocksocial.shell.rememberShellNavigator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferences: PreferencesRepository

    @Inject
    lateinit var probe: ProtectionHealthProbe

    @Inject
    lateinit var dashboard: DashboardController

    @Inject
    lateinit var apps: AppSelectionController

    @Inject
    lateinit var rules: RulesController

    @Inject
    lateinit var history: HistoryController

    private var setupDone by mutableStateOf<Boolean?>(null)
    private var stage by mutableStateOf(OnboardingFlow.start)
    private var health by mutableStateOf(ProtectionHealth.Unknown)

    private val notificationRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refreshHealth() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch { setupDone = preferences.preferences.first().onboardingCompleted }
        setContent {
            BlockSocialTheme {
                when (setupDone) {
                    null -> Unit
                    false -> Setup()
                    true -> Main()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        probe.startProbe()
        refreshHealth()
    }

    @Composable
    private fun Setup() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.xl),
        ) {
            OnboardingScreen(
                stage = stage,
                statuses = health.statuses,
                onNext = { stage = OnboardingFlow.next(stage) ?: stage },
                onBack = { stage = OnboardingFlow.back(stage) ?: stage },
                onSkipIntro = { stage = OnboardingFlow.skipIntro() },
                onRequest = ::request,
                onFinish = ::finishSetup,
            )
        }
    }

    @Composable
    private fun Main() {
        val navigator = rememberShellNavigator()

        BackHandler(enabled = navigator.canGoBack) { navigator.back() }

        BlockSocialShell(navigator = navigator) { destination ->
            when (destination) {
                Destination.Dashboard -> Dashboard(navigator)
                Destination.Apps -> Apps(navigator)
                Destination.History -> History()
                Destination.Protection -> Protection()
                is Destination.Rules -> Rules(destination.app)
            }
        }
    }

    @Composable
    private fun Dashboard(navigator: ShellNavigator) {
        val states = remember { dashboard.state(probe.observe().map { it.banner }) }
        val state by states.collectAsState(initial = DashboardState.Empty)

        ScrollingScreen {
            DashboardScreen(
                state = state,
                onOpenProtection = { navigator.open(Destination.Protection) },
            )
        }
    }

    @Composable
    private fun Apps(navigator: ShellNavigator) {
        val rows by apps.rows().collectAsState(initial = emptyList())

        AppSelectionScreen(
            rows = rows,
            onSelectedChange = { ref, selected ->
                lifecycleScope.launch { apps.setSelected(ref, selected) }
            },
            onOpenRules = { row -> navigator.open(Destination.Rules(row.ref, row.displayName)) },
        )
    }

    @Composable
    private fun History() {
        val page by history.page().collectAsState(initial = HistoryPage.Empty)

        ScrollsItselfScreen {
            HistoryScreen(events = page.events, displayNameOf = page::displayNameOf)
        }
    }

    @Composable
    private fun Protection() {
        ScrollingScreen {
            ProtectionHealthScreen(
                items = health.items,
                blockingRuns = health.blockingRuns,
                onRepair = ::openSettingsFor,
            )
        }
    }

    @Composable
    private fun Rules(app: AppRef) {
        val saved by rules.rulesFor(app).collectAsState(initial = emptyList())
        var draft by remember(app) { mutableStateOf(rules.newDraft(RuleMode.SCHEDULE)) }

        ScrollingScreen {
            RuleListScreen(
                rules = saved,
                onEnabledChange = { rule, enabled ->
                    lifecycleScope.launch { rules.setEnabled(rule, app, enabled) }
                },
                onEdit = { draft = RuleDraft.of(it) },
            )
            RuleEditorScreen(
                draft = draft,
                onDraftChange = { draft = it },
                onSave = {
                    lifecycleScope.launch {
                        rules.save(draft, app)
                        draft = rules.newDraft(RuleMode.SCHEDULE)
                    }
                },
            )
        }
    }

    private fun refreshHealth() {
        lifecycleScope.launch {
            health = probe.read()
            if (probe.notificationAsk(health) != NotificationAsk.ASK_NOW) return@launch
            probe.rememberNotificationRequest()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openSettingsFor(ProtectionRequirement.NOTIFICATIONS)
            }
        }
    }

    private fun request(requirement: ProtectionRequirement) {
        when (requirement) {
            ProtectionRequirement.ACCESSIBILITY_SERVICE -> requestAccessibility()
            ProtectionRequirement.USAGE_ACCESS -> openSettingsFor(requirement)
            ProtectionRequirement.NOTIFICATIONS -> Unit
        }
    }

    private fun requestAccessibility() {
        if (stage != OnboardingStage.ACCESSIBILITY_CONSENT) {
            stage = OnboardingFlow.request(ProtectionRequirement.ACCESSIBILITY_SERVICE) ?: stage
            return
        }
        lifecycleScope.launch { preferences.setAcceptedConsentVersion(CONSENT_VERSION) }
        stage = OnboardingFlow.next(stage) ?: stage
        openSettingsFor(ProtectionRequirement.ACCESSIBILITY_SERVICE)
    }

    private fun finishSetup() {
        lifecycleScope.launch {
            preferences.setOnboardingCompleted(true)
            setupDone = true
        }
    }

    private fun openSettingsFor(requirement: ProtectionRequirement) {
        val action = when (requirement) {
            ProtectionRequirement.ACCESSIBILITY_SERVICE -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            ProtectionRequirement.USAGE_ACCESS -> Settings.ACTION_USAGE_ACCESS_SETTINGS
            ProtectionRequirement.NOTIFICATIONS -> Settings.ACTION_APP_NOTIFICATION_SETTINGS
        }
        val intent = Intent(action).apply {
            if (requirement == ProtectionRequirement.NOTIFICATIONS) {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        }
        runCatching { startActivity(intent) }
    }

    private companion object {
        const val CONSENT_VERSION = 1
    }
}
