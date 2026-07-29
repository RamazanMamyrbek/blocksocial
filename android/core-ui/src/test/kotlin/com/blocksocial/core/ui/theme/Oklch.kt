package com.blocksocial.core.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

data class Oklch(val lightness: Double, val chroma: Double, val hueDegrees: Double)

fun Oklch.toColor(): Color {
    val hueRadians = hueDegrees * PI / 180.0
    val a = chroma * cos(hueRadians)
    val b = chroma * sin(hueRadians)
    val longWave = (lightness + 0.3963377774 * a + 0.2158037573 * b).pow(3)
    val mediumWave = (lightness - 0.1055613458 * a - 0.0638541728 * b).pow(3)
    val shortWave = (lightness - 0.0894841775 * a - 1.2914855480 * b).pow(3)
    val red = 4.0767416621 * longWave - 3.3077115913 * mediumWave + 0.2309699292 * shortWave
    val green = -1.2684380046 * longWave + 2.6097574011 * mediumWave - 0.3413193965 * shortWave
    val blue = -0.0041960863 * longWave - 0.7034186147 * mediumWave + 1.7076147010 * shortWave
    val argb = (0xFFL shl 24) or (encode(red) shl 16) or (encode(green) shl 8) or encode(blue)
    return Color(argb)
}

private fun encode(linearChannel: Double): Long {
    val clamped = linearChannel.coerceIn(0.0, 1.0)
    val gammaEncoded =
        if (clamped <= 0.0031308) 12.92 * clamped else 1.055 * clamped.pow(1.0 / 2.4) - 0.055
    return (gammaEncoded * 255.0).roundToInt().toLong()
}

fun Color.relativeLuminance(): Double =
    0.2126 * linearize(red) + 0.7152 * linearize(green) + 0.0722 * linearize(blue)

private fun linearize(channel: Float): Double {
    val value = channel.toDouble()
    return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
}

fun contrastRatio(foreground: Color, background: Color): Double {
    val first = foreground.relativeLuminance()
    val second = background.relativeLuminance()
    return (max(first, second) + 0.05) / (min(first, second) + 0.05)
}
