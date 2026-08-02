package com.blocksocial.lite.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val DisplaySmall = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 32.sp,
    fontWeight = FontWeight.Bold,
)

val TitleLarge = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 22.sp,
    fontWeight = FontWeight.SemiBold,
)

val LabelLarge = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 16.sp,
    fontWeight = FontWeight.SemiBold,
)

val BodyLarge = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 15.sp,
    fontWeight = FontWeight.Normal,
)

val Numeric = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 13.sp,
    fontWeight = FontWeight.Medium,
)

val BlockSocialTypography = Typography(
    headlineMedium = DisplaySmall,
    titleLarge = TitleLarge,
    labelLarge = LabelLarge,
    bodyLarge = BodyLarge,
)
