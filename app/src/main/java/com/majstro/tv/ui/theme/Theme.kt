package com.majstro.tv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.majstro.tv.R

/** Midnight-navy & warm-champagne editorial color tokens for Majstro. */
object MajstroColors {
    val MidnightBg = Color(0xFF0E151C)
    val SlateSurface = Color(0xFF141E28)
    val SlateElevated = Color(0xFF1A2532)
    val Hairline = Color(0xFF23303E)
    val Champagne = Color(0xFFDFB49B)
    val ChampagneBright = Color(0xFFECC8B2)
    val SageGreen = Color(0xFF4E9F76)
    val SpotifyGreen = Color(0xFF25B864)
    val WarmIvory = Color(0xFFF2EFEA)
    val Parchment = Color(0xFFEDE7DF)
    val CoolMuted = Color(0xFF8E9BA8)
    val DeepInk = Color(0xFF10171E)
}

@OptIn(ExperimentalTextApi::class)
val CormorantGaramondFamily = FontFamily(
    Font(
        resId = R.font.cormorant_garamond,
        weight = FontWeight.Normal,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        resId = R.font.cormorant_garamond,
        weight = FontWeight.Medium,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        resId = R.font.cormorant_garamond,
        weight = FontWeight.SemiBold,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        resId = R.font.cormorant_garamond_italic,
        weight = FontWeight.Normal,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        resId = R.font.cormorant_garamond_italic,
        weight = FontWeight.Medium,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    )
)

@OptIn(ExperimentalTextApi::class)
val InterFamily = FontFamily(
    Font(
        resId = R.font.inter,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        resId = R.font.inter,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        resId = R.font.inter,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        resId = R.font.inter,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    )
)

val MajstroTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = CormorantGaramondFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 42.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = CormorantGaramondFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = CormorantGaramondFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = CormorantGaramondFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 21.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 1.6.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.8.sp
    )
)

private val MajstroDarkColorScheme = darkColorScheme(
    primary = MajstroColors.Champagne,
    onPrimary = MajstroColors.DeepInk,
    surface = MajstroColors.SlateSurface,
    onSurface = MajstroColors.WarmIvory,
    background = MajstroColors.MidnightBg,
    onBackground = MajstroColors.WarmIvory,
    outline = MajstroColors.Hairline
)

/** Applies Majstro's Midnight Classical MaterialTheme. */
@Composable
fun MajstroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MajstroDarkColorScheme,
        typography = MajstroTypography,
        content = content
    )
}
