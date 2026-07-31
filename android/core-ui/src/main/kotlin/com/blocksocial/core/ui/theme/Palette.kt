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
    surface = Color(0xFF101512),
    surfaceContainer = Color(0xFF1C241F),
    surfaceContainerHigh = Color(0xFF27322C),
    onSurface = Color(0xFFE2EAE5),
    onSurfaceVariant = Color(0xFF9DA7A2),
    outline = Color(0xFF57605B),
    primary = Color(0xFF71D9AE),
    onPrimary = Color(0xFF101512),
    success = Color(0xFF81D7A0),
    warning = Color(0xFFE3B97E),
    danger = Color(0xFFE58C84),
)

val LightPalette = BlockSocialPalette(
    surface = Color(0xFFF2F6F2),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFE4ECE5),
    onSurface = Color(0xFF15211A),
    onSurfaceVariant = Color(0xFF555F59),
    outline = Color(0xFFC5CFC9),
    primary = Color(0xFF1E7557),
    onPrimary = Color(0xFFFFFFFF),
    success = Color(0xFF30744C),
    warning = Color(0xFF9A620A),
    danger = Color(0xFFAD3B39),
)

val LocalBlockSocialPalette = staticCompositionLocalOf { DarkPalette }
