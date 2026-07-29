package com.blocksocial.core.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorTokenConversionTest {

    private val darkSources = listOf(
        Triple("surface", Oklch(0.20, 0.012, 240.0), DarkPalette.surface),
        Triple("surfaceContainer", Oklch(0.245, 0.014, 240.0), DarkPalette.surfaceContainer),
        Triple("surfaceContainerHigh", Oklch(0.30, 0.016, 240.0), DarkPalette.surfaceContainerHigh),
        Triple("onSurface", Oklch(0.94, 0.006, 240.0), DarkPalette.onSurface),
        Triple("onSurfaceVariant", Oklch(0.73, 0.012, 240.0), DarkPalette.onSurfaceVariant),
        Triple("outline", Oklch(0.45, 0.012, 240.0), DarkPalette.outline),
        Triple("primary", Oklch(0.75, 0.078, 196.0), DarkPalette.primary),
        Triple("success", Oklch(0.74, 0.070, 158.0), DarkPalette.success),
        Triple("warning", Oklch(0.80, 0.082, 78.0), DarkPalette.warning),
        Triple("danger", Oklch(0.66, 0.105, 26.0), DarkPalette.danger),
    )

    private val lightSources = listOf(
        Triple("surface", Oklch(0.978, 0.004, 240.0), LightPalette.surface),
        Triple("surfaceContainer", Oklch(0.945, 0.006, 240.0), LightPalette.surfaceContainer),
        Triple("surfaceContainerHigh", Oklch(0.905, 0.008, 240.0), LightPalette.surfaceContainerHigh),
        Triple("onSurface", Oklch(0.245, 0.014, 240.0), LightPalette.onSurface),
        Triple("onSurfaceVariant", Oklch(0.475, 0.013, 240.0), LightPalette.onSurfaceVariant),
        Triple("outline", Oklch(0.795, 0.009, 240.0), LightPalette.outline),
        Triple("primary", Oklch(0.50, 0.075, 196.0), LightPalette.primary),
        Triple("success", Oklch(0.48, 0.068, 158.0), LightPalette.success),
        Triple("warning", Oklch(0.60, 0.080, 68.0), LightPalette.warning),
        Triple("danger", Oklch(0.52, 0.115, 26.0), LightPalette.danger),
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
