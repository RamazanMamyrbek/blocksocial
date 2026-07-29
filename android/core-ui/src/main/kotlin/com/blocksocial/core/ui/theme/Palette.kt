package com.blocksocial.core.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class BlockSocialPalette(
    val surface: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val primary: Color,
    val onPrimary: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
)

val DarkPalette = BlockSocialPalette(
    surface = Color(0xFF11171B),
    surfaceContainer = Color(0xFF1B2127),
    surfaceContainerHigh = Color(0xFF272F35),
    onSurface = Color(0xFFE8ECEF),
    onSurfaceVariant = Color(0xFFA1A9AF),
    outline = Color(0xFF50565B),
    primary = Color(0xFF6FBEBE),
    onPrimary = Color(0xFF11171B),
    success = Color(0xFF85B99A),
    warning = Color(0xFFDBB881),
    danger = Color(0xFFCB7870),
)

val LightPalette = BlockSocialPalette(
    surface = Color(0xFFF5F8FA),
    surfaceContainer = Color(0xFFE9EDF0),
    surfaceContainerHigh = Color(0xFFDBE0E4),
    onSurface = Color(0xFF1B2127),
    onSurfaceVariant = Color(0xFF565D63),
    outline = Color(0xFFB7BDC1),
    primary = Color(0xFF207071),
    onPrimary = Color(0xFFFFFFFF),
    success = Color(0xFF3A694F),
    warning = Color(0xFFA0774A),
    danger = Color(0xFFA14B45),
)

val LocalBlockSocialPalette = staticCompositionLocalOf { DarkPalette }
