package com.blocksocial.core.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorContrastTest {

    private val themes = listOf("dark" to DarkPalette, "light" to LightPalette)

    private fun BlockSocialPalette.surfaceSteps(): List<Pair<String, Color>> = listOf(
        "surface" to surface,
        "surfaceContainer" to surfaceContainer,
        "surfaceContainerHigh" to surfaceContainerHigh,
    )

    @Test
    fun `every on-color clears 4_5 to 1 against every surface step`() {
        themes.forEach { (theme, palette) ->
            val textRoles = listOf(
                "onSurface" to palette.onSurface,
                "onSurfaceVariant" to palette.onSurfaceVariant,
            )
            textRoles.forEach { (role, foreground) ->
                palette.surfaceSteps().forEach { (step, background) ->
                    val ratio = contrastRatio(foreground, background)
                    assertTrue(
                        "$theme $role on $step is $ratio",
                        ratio >= 4.5,
                    )
                }
            }
        }
    }

    @Test
    fun `on-primary and on-error clear 4_5 to 1 against their own fill`() {
        themes.forEach { (theme, palette) ->
            val onPrimary = contrastRatio(palette.onPrimary, palette.primary)
            assertTrue("$theme onPrimary on primary is $onPrimary", onPrimary >= 4.5)

            val onError = contrastRatio(palette.onPrimary, palette.danger)
            assertTrue("$theme onError on danger is $onError", onError >= 4.5)
        }
    }

    @Test
    fun `accent roles clear the 3 to 1 non-text minimum against every surface step`() {
        themes.forEach { (theme, palette) ->
            val accents = listOf(
                "primary" to palette.primary,
                "success" to palette.success,
                "warning" to palette.warning,
                "danger" to palette.danger,
            )
            accents.forEach { (role, accent) ->
                palette.surfaceSteps().forEach { (step, background) ->
                    val ratio = contrastRatio(accent, background)
                    assertTrue("$theme $role on $step is $ratio", ratio >= 3.0)
                }
            }
        }
    }

    @Test
    fun `outline is distinguishable from every surface step`() {
        themes.forEach { (theme, palette) ->
            palette.surfaceSteps().forEach { (step, background) ->
                assertNotEquals("$theme outline equals $step", palette.outline, background)
            }
        }
    }
}
