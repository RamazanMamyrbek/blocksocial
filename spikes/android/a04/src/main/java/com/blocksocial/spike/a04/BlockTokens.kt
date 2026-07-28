package com.blocksocial.spike.a04

import androidx.compose.ui.graphics.Color

data class BlockPalette(
    val surface: Color,
    val surfaceContainer: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val primary: Color,
    val onPrimary: Color
)

private val Dark = BlockPalette(
    surface = Color(0xFF11171B),
    surfaceContainer = Color(0xFF1B2127),
    onSurface = Color(0xFFE8ECEF),
    onSurfaceVariant = Color(0xFFA1A9AF),
    outline = Color(0xFF50565B),
    primary = Color(0xFF6FBEBE),
    onPrimary = Color(0xFF11171B)
)

private val Light = BlockPalette(
    surface = Color(0xFFF5F8FA),
    surfaceContainer = Color(0xFFE9EDF0),
    onSurface = Color(0xFF1B2127),
    onSurfaceVariant = Color(0xFF565D63),
    outline = Color(0xFFB7BDC1),
    primary = Color(0xFF207071),
    onPrimary = Color(0xFFFFFFFF)
)

fun blockPalette(darkTheme: Boolean): BlockPalette = if (darkTheme) Dark else Light
