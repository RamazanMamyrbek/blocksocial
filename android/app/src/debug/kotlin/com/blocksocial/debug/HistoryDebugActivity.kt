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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.blocksocial.core.ui.theme.BlockSocialTheme
import com.blocksocial.core.ui.theme.Spacing
import com.blocksocial.feature.history.HistoryController
import com.blocksocial.feature.history.HistoryPage
import com.blocksocial.feature.history.HistoryScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HistoryDebugActivity : ComponentActivity() {

    @Inject
    lateinit var controller: HistoryController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlockSocialTheme {
                val page by controller.page().collectAsState(initial = HistoryPage.Empty)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .safeDrawingPadding()
                        .padding(Spacing.xl),
                ) {
                    HistoryScreen(events = page.events, displayNameOf = page::displayNameOf)
                }
            }
        }
    }
}
