package com.kyly.picking.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kyly.picking.R

val PublicSans = FontFamily(
    Font(R.font.publicsans_regular,   FontWeight.Normal),
    Font(R.font.publicsans_medium,    FontWeight.Medium),
    Font(R.font.publicsans_semibold,  FontWeight.SemiBold),
    Font(R.font.publicsans_bold,      FontWeight.Bold),
    Font(R.font.publicsans_extrabold, FontWeight.ExtraBold),
    Font(R.font.publicsans_black,     FontWeight.Black),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrainsmono_bold, FontWeight.Bold),
)

val NumeralXl = TextStyle(
    fontFamily = JetBrainsMono,
    fontWeight = FontWeight.Bold,
    fontSize = 72.sp,
    lineHeight = 80.sp,
)

val NumeralLarge = TextStyle(
    fontFamily = PublicSans,
    fontWeight = FontWeight.Black,
    fontSize = 48.sp,
    lineHeight = 56.sp,
)

val KylyTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
)
