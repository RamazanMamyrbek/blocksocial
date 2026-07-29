package com.blocksocial.debug

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blocksocial.core.ui.theme.LocalBlockSocialPalette
import com.blocksocial.core.ui.theme.LocalMotionDurations
import com.blocksocial.core.ui.theme.Numeric
import com.blocksocial.core.ui.theme.Radius
import com.blocksocial.core.ui.theme.Spacing

@Composable
fun TokenPreviewScreen() {
    val palette = LocalBlockSocialPalette.current
    val motion = LocalMotionDurations.current
    val themeName = if (isSystemInDarkTheme()) "dark" else "light"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Text(
            text = "Design tokens",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = themeName,
            style = Numeric,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionTitle("Color")
        Swatch("surface", palette.surface)
        Swatch("surfaceContainer", palette.surfaceContainer)
        Swatch("surfaceContainerHigh", palette.surfaceContainerHigh)
        Swatch("onSurface", palette.onSurface)
        Swatch("onSurfaceVariant", palette.onSurfaceVariant)
        Swatch("outline", palette.outline)
        Swatch("primary", palette.primary)
        Swatch("onPrimary", palette.onPrimary)
        Swatch("success", palette.success)
        Swatch("warning", palette.warning)
        Swatch("danger", palette.danger)

        SectionTitle("Type")
        Text(
            text = "display-sm 32/700",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "title-lg 22/600",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "label-lg 16/600",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "body-lg 15/400",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "numeric 13/500 mono",
            style = Numeric,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionTitle("Spacing")
        listOf(
            "4" to Spacing.xs,
            "8" to Spacing.sm,
            "12" to Spacing.md,
            "16" to Spacing.lg,
            "24" to Spacing.xl,
            "32" to Spacing.xxl,
            "48" to Spacing.xxxl,
        ).forEach { (label, value) -> ScaleBar(label, value) }

        SectionTitle("Radius")
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            RadiusSample("4", RoundedCornerShape(Radius.xs))
            RadiusSample("8", RoundedCornerShape(Radius.sm))
            RadiusSample("14", RoundedCornerShape(Radius.md))
            RadiusSample("20", RoundedCornerShape(Radius.lg))
            RadiusSample("full", Radius.full)
        }

        SectionTitle("Motion")
        Text(
            text = "block-appear ${motion.blockAppearMillis} ms",
            style = Numeric,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "state-change ${motion.stateChangeMillis} ms",
            style = Numeric,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "sheet ${motion.sheetMillis} ms",
            style = Numeric,
            color = MaterialTheme.colorScheme.onSurface,
        )
        MotionSample(durationMillis = motion.stateChangeMillis)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun Swatch(role: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(Spacing.xxxl)
                .background(color, RoundedCornerShape(Radius.sm))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(Radius.sm)),
        )
        Column {
            Text(
                text = role,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = color.toHex(),
                style = Numeric,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ScaleBar(label: String, value: Dp) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = Numeric,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(Spacing.xxl),
        )
        Box(
            modifier = Modifier
                .width(value)
                .height(Spacing.md)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun RadiusSample(label: String, shape: RoundedCornerShape) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(Spacing.xxxl)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape),
        )
        Text(
            text = label,
            style = Numeric,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MotionSample(durationMillis: Int) {
    var expanded by remember { mutableStateOf(false) }
    val width by animateDpAsState(
        targetValue = if (expanded) 240.dp else 64.dp,
        animationSpec = tween(durationMillis = durationMillis),
        label = "motion-sample",
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Box(
            modifier = Modifier
                .width(width)
                .height(Spacing.xl)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(Radius.sm)),
        )
        Button(onClick = { expanded = !expanded }) {
            Text(text = "Animate")
        }
    }
}

private fun Color.toHex(): String = String.format("#%06X", toArgb() and 0xFFFFFF)
