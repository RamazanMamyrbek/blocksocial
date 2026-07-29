package com.blocksocial.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

fun BlockSocialPalette.toColorScheme(surfaceBright: Color, surfaceDim: Color): ColorScheme =
    ColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = surfaceContainerHigh,
        onPrimaryContainer = onSurface,
        inversePrimary = primary,
        secondary = primary,
        onSecondary = onPrimary,
        secondaryContainer = surfaceContainer,
        onSecondaryContainer = onSurface,
        tertiary = primary,
        onTertiary = onPrimary,
        tertiaryContainer = surfaceContainer,
        onTertiaryContainer = onSurface,
        background = surface,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceContainer,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = primary,
        inverseSurface = onSurface,
        inverseOnSurface = surface,
        error = danger,
        onError = onPrimary,
        errorContainer = surfaceContainerHigh,
        onErrorContainer = onSurface,
        outline = outline,
        outlineVariant = surfaceContainerHigh,
        scrim = Color.Black,
        surfaceBright = surfaceBright,
        surfaceDim = surfaceDim,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHigh,
        surfaceContainerLow = surfaceContainer,
        surfaceContainerLowest = surface,
        primaryFixed = primary,
        primaryFixedDim = primary,
        onPrimaryFixed = onPrimary,
        onPrimaryFixedVariant = onPrimary,
        secondaryFixed = primary,
        secondaryFixedDim = primary,
        onSecondaryFixed = onPrimary,
        onSecondaryFixedVariant = onPrimary,
        tertiaryFixed = primary,
        tertiaryFixedDim = primary,
        onTertiaryFixed = onPrimary,
        onTertiaryFixedVariant = onPrimary,
    )

val DarkColorScheme = DarkPalette.toColorScheme(
    surfaceBright = DarkPalette.surfaceContainerHigh,
    surfaceDim = DarkPalette.surface,
)

val LightColorScheme = LightPalette.toColorScheme(
    surfaceBright = LightPalette.surface,
    surfaceDim = LightPalette.surfaceContainerHigh,
)

@Composable
fun BlockSocialTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalBlockSocialPalette provides if (darkTheme) DarkPalette else LightPalette,
        LocalMotionDurations provides rememberMotionDurations(),
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = BlockSocialTypography,
            shapes = BlockSocialShapes,
            content = content,
        )
    }
}
