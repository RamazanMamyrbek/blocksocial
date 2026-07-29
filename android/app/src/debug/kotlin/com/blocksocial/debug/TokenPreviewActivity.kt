package com.blocksocial.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.blocksocial.core.ui.theme.BlockSocialTheme

class TokenPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlockSocialTheme {
                TokenPreviewScreen()
            }
        }
    }
}
