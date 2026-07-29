package com.blocksocial.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorSchemeTest {

    private fun ColorScheme.roles(): Map<String, Color> = mapOf(
        "primary" to primary,
        "onPrimary" to onPrimary,
        "primaryContainer" to primaryContainer,
        "onPrimaryContainer" to onPrimaryContainer,
        "inversePrimary" to inversePrimary,
        "secondary" to secondary,
        "onSecondary" to onSecondary,
        "secondaryContainer" to secondaryContainer,
        "onSecondaryContainer" to onSecondaryContainer,
        "tertiary" to tertiary,
        "onTertiary" to onTertiary,
        "tertiaryContainer" to tertiaryContainer,
        "onTertiaryContainer" to onTertiaryContainer,
        "background" to background,
        "onBackground" to onBackground,
        "surface" to surface,
        "onSurface" to onSurface,
        "surfaceVariant" to surfaceVariant,
        "onSurfaceVariant" to onSurfaceVariant,
        "surfaceTint" to surfaceTint,
        "inverseSurface" to inverseSurface,
        "inverseOnSurface" to inverseOnSurface,
        "error" to error,
        "onError" to onError,
        "errorContainer" to errorContainer,
        "onErrorContainer" to onErrorContainer,
        "outline" to outline,
        "outlineVariant" to outlineVariant,
        "scrim" to scrim,
        "surfaceBright" to surfaceBright,
        "surfaceDim" to surfaceDim,
        "surfaceContainer" to surfaceContainer,
        "surfaceContainerHigh" to surfaceContainerHigh,
        "surfaceContainerHighest" to surfaceContainerHighest,
        "surfaceContainerLow" to surfaceContainerLow,
        "surfaceContainerLowest" to surfaceContainerLowest,
    )

    private fun BlockSocialPalette.colors(): Set<Color> = setOf(
        surface,
        surfaceContainer,
        surfaceContainerHigh,
        onSurface,
        onSurfaceVariant,
        outline,
        primary,
        onPrimary,
        success,
        warning,
        danger,
        Color.Black,
    )

    @Test
    fun `every material role in both themes comes from the documented palette`() {
        listOf(
            Triple("dark", DarkColorScheme, DarkPalette),
            Triple("light", LightColorScheme, LightPalette),
        ).forEach { (theme, scheme, palette) ->
            val allowed = palette.colors()
            scheme.roles().forEach { (role, color) ->
                assertTrue("$theme $role is $color, which is not a documented token", color in allowed)
            }
        }
    }

    @Test
    fun `elevation is a surface step rather than a shadow`() {
        listOf(DarkColorScheme, LightColorScheme).forEach { scheme ->
            assertTrue(scheme.surface != scheme.surfaceContainer)
            assertTrue(scheme.surfaceContainer != scheme.surfaceContainerHigh)
        }
    }

    @Test
    fun `the two themes share no surface value`() {
        assertTrue(DarkPalette.surface != LightPalette.surface)
        assertTrue(DarkPalette.onSurface != LightPalette.onSurface)
    }
}
