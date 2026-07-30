package com.blocksocial.debug

import android.os.Bundle
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.blocksocial.core.ui.theme.BlockSocialTheme
import com.blocksocial.core.ui.theme.Spacing
import com.blocksocial.feature.dashboard.DashboardController
import com.blocksocial.feature.dashboard.DashboardScreen
import com.blocksocial.feature.dashboard.DashboardState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DashboardDebugActivity : ComponentActivity() {

    @Inject
    lateinit var controller: DashboardController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlockSocialTheme {
                val state by controller.state().collectAsState(initial = DashboardState.Empty)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .safeDrawingPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.xl),
                ) {
                    DashboardScreen(state)
                }
            }
        }
    }
}
