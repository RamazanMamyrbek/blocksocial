package com.blocksocial.spike.a02

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BlockScreen(
    appLabel: String,
    ruleHeadline: String,
    supportingText: String,
    footnote: String,
    stayFocusedLabel: String,
    openTemporarilyLabel: String,
    onStayFocused: () -> Unit,
    onOpenTemporarily: () -> Unit,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val palette = blockPalette(darkTheme)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = appLabel,
            color = palette.onSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 20.sp
        )

        Spacer(Modifier.padding(top = 12.dp))

        Text(
            text = ruleHeadline,
            color = palette.onSurface,
            fontSize = 34.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.semantics { heading() }
        )

        Spacer(Modifier.padding(top = 16.dp))

        Text(
            text = supportingText,
            color = palette.onSurfaceVariant,
            fontSize = 16.sp,
            lineHeight = 24.sp
        )

        Spacer(Modifier.padding(top = 40.dp))

        Button(
            onClick = onStayFocused,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.primary,
                contentColor = palette.onPrimary
            )
        ) {
            Text(
                text = stayFocusedLabel,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.padding(top = 12.dp))

        OutlinedButton(
            onClick = onOpenTemporarily,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = RoundedCornerShape(16.dp),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                brush = SolidColor(palette.outline)
            ),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.onSurface)
        ) {
            Text(
                text = openTemporarilyLabel,
                fontSize = 17.sp,
                lineHeight = 22.sp
            )
        }

        Spacer(Modifier.padding(top = 28.dp))

        Text(
            text = footnote,
            color = palette.onSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.Start)
        )
    }
}
