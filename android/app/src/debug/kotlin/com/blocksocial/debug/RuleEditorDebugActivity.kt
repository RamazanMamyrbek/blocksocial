package com.blocksocial.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.RuleMode
import com.blocksocial.core.ui.theme.BlockSocialTheme
import com.blocksocial.core.ui.theme.Spacing
import com.blocksocial.feature.rules.RuleDraft
import com.blocksocial.feature.rules.RuleEditorScreen
import com.blocksocial.feature.rules.RuleListScreen
import com.blocksocial.feature.rules.RulesController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RuleEditorDebugActivity : ComponentActivity() {

    @Inject
    lateinit var controller: RulesController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlockSocialTheme {
                val scope = rememberCoroutineScope()
                val rules by controller.rulesFor(APP).collectAsState(initial = emptyList())
                var draft by remember { mutableStateOf(controller.newDraft(RuleMode.SCHEDULE)) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .safeDrawingPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                ) {
                    RuleListScreen(
                        rules = rules,
                        onEnabledChange = { rule, enabled ->
                            scope.launch { controller.setEnabled(rule, APP, enabled) }
                        },
                        onEdit = { draft = RuleDraft.of(it) },
                    )
                    RuleEditorScreen(
                        draft = draft,
                        onDraftChange = { draft = it },
                        onSave = {
                            scope.launch {
                                controller.save(draft, APP)
                                draft = controller.newDraft(RuleMode.SCHEDULE)
                            }
                        },
                    )
                }
            }
        }
    }

    private companion object {
        val APP = AppRef("youtube")
    }
}
