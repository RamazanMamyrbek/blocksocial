package com.blocksocial.debug

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.blocksocial.core.data.preferences.PreferencesRepository
import com.blocksocial.core.domain.ProtectionRequirement
import com.blocksocial.core.domain.RequirementStatus
import com.blocksocial.core.ui.theme.BlockSocialTheme
import com.blocksocial.core.ui.theme.Spacing
import com.blocksocial.feature.onboarding.OnboardingFlow
import com.blocksocial.feature.onboarding.OnboardingScreen
import com.blocksocial.feature.onboarding.OnboardingStage
import com.blocksocial.health.ProtectionHealthProbe
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingDebugActivity : ComponentActivity() {

    @Inject
    lateinit var probe: ProtectionHealthProbe

    @Inject
    lateinit var preferences: PreferencesRepository

    private var stage by mutableStateOf(OnboardingFlow.start)
    private var statuses by mutableStateOf(emptyMap<ProtectionRequirement, RequirementStatus>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlockSocialTheme {
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
                        statuses = statuses,
                        onNext = { stage = OnboardingFlow.next(stage) ?: stage },
                        onBack = { stage = OnboardingFlow.back(stage) ?: stage },
                        onSkipIntro = { stage = OnboardingFlow.skipIntro() },
                        onRequest = ::request,
                        onFinish = ::finishSetup,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        probe.startProbe()
        lifecycleScope.launch { statuses = probe.read().statuses }
    }

    private fun request(requirement: ProtectionRequirement) {
        when (requirement) {
            ProtectionRequirement.ACCESSIBILITY_SERVICE -> requestAccessibility()
            ProtectionRequirement.USAGE_ACCESS -> open(Settings.ACTION_USAGE_ACCESS_SETTINGS)
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
        open(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }

    private fun finishSetup() {
        lifecycleScope.launch {
            preferences.setOnboardingCompleted(true)
            finish()
        }
    }

    private fun open(action: String) {
        runCatching { startActivity(Intent(action)) }
    }

    private companion object {
        const val CONSENT_VERSION = 1
    }
}
