package com.blocksocial.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.blocksocial.core.ui.theme.BlockSocialTheme
import com.blocksocial.feature.appselection.AppSelectionController
import com.blocksocial.feature.appselection.AppSelectionScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppSelectionDebugActivity : ComponentActivity() {

    @Inject
    lateinit var controller: AppSelectionController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlockSocialTheme {
                val rows by controller.rows().collectAsState(initial = emptyList())
                val scope = rememberCoroutineScope()
                AppSelectionScreen(
                    rows = rows,
                    onSelectedChange = { ref, selected ->
                        scope.launch { controller.setSelected(ref, selected) }
                    },
                )
            }
        }
    }
}
