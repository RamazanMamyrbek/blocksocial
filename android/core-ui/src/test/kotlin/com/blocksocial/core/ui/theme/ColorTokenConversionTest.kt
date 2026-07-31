package com.blocksocial.core.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorTokenConversionTest {

    private val darkSources = listOf(
        Triple("surface", Oklch(0.190, 0.010, 160.0), DarkPalette.surface),
        Triple("surfaceContainer", Oklch(0.250, 0.014, 160.0), DarkPalette.surfaceContainer),
        Triple("surfaceContainerHigh", Oklch(0.305, 0.018, 160.0), DarkPalette.surfaceContainerHigh),
        Triple("onSurface", Oklch(0.930, 0.010, 160.0), DarkPalette.onSurface),
        Triple("onSurfaceVariant", Oklch(0.720, 0.014, 160.0), DarkPalette.onSurfaceVariant),
        Triple("outline", Oklch(0.480, 0.014, 160.0), DarkPalette.outline),
        Triple("primary", Oklch(0.810, 0.115, 165.0), DarkPalette.primary),
        Triple("success", Oklch(0.810, 0.115, 155.0), DarkPalette.success),
        Triple("warning", Oklch(0.810, 0.090, 75.0), DarkPalette.warning),
        Triple("danger", Oklch(0.730, 0.110, 25.0), DarkPalette.danger),
    )

    private val lightSources = listOf(
        Triple("surface", Oklch(0.968, 0.006, 150.0), LightPalette.surface),
        Triple("surfaceContainer", Oklch(1.000, 0.000, 150.0), LightPalette.surfaceContainer),
        Triple("surfaceContainerHigh", Oklch(0.935, 0.012, 150.0), LightPalette.surfaceContainerHigh),
        Triple("onSurface", Oklch(0.235, 0.022, 160.0), LightPalette.onSurface),
        Triple("onSurfaceVariant", Oklch(0.475, 0.016, 160.0), LightPalette.onSurfaceVariant),
        Triple("outline", Oklch(0.845, 0.014, 160.0), LightPalette.outline),
        Triple("primary", Oklch(0.505, 0.095, 165.0), LightPalette.primary),
        Triple("success", Oklch(0.505, 0.095, 155.0), LightPalette.success),
        Triple("warning", Oklch(0.545, 0.115, 70.0), LightPalette.warning),
        Triple("danger", Oklch(0.515, 0.150, 25.0), LightPalette.danger),
    )

    @Test
    fun `dark tokens equal their documented oklch sources`() {
        darkSources.forEach { (role, source, token) ->
            assertEquals("dark $role", source.toColor(), token)
        }
    }

    @Test
    fun `light tokens equal their documented oklch sources`() {
        lightSources.forEach { (role, source, token) ->
            assertEquals("light $role", source.toColor(), token)
        }
    }

    @Test
    fun `derived on-primary follows the recorded derivation`() {
        assertEquals(DarkPalette.surface, DarkPalette.onPrimary)
        assertEquals(Color.White, LightPalette.onPrimary)
    }
}
