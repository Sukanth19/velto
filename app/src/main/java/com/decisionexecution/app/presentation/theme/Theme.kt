package com.decisionexecution.app.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.decisionexecution.app.R
import com.decisionexecution.app.domain.model.ThemeMode
import com.decisionexecution.app.domain.model.ThemePalette

// Plus Jakarta Sans (SIL OFL, see third_party/), one variable font file for every weight
@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: Int) = Font(
    R.font.plus_jakarta_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

private val Jakarta = FontFamily(jakarta(400), jakarta(500), jakarta(600), jakarta(700), jakarta(800))

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Jakarta, fontSize = 64.sp, fontWeight = FontWeight.W300, letterSpacing = (-2).sp),
    displaySmall = TextStyle(fontFamily = Jakarta, fontSize = 34.sp, fontWeight = FontWeight.W700, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = Jakarta, fontSize = 26.sp, fontWeight = FontWeight.W700, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontFamily = Jakarta, fontSize = 22.sp, fontWeight = FontWeight.W700),
    titleLarge = TextStyle(fontFamily = Jakarta, fontSize = 20.sp, fontWeight = FontWeight.W700),
    titleMedium = TextStyle(fontFamily = Jakarta, fontSize = 16.sp, fontWeight = FontWeight.W600),
    titleSmall = TextStyle(fontFamily = Jakarta, fontSize = 14.sp, fontWeight = FontWeight.W600),
    bodyLarge = TextStyle(fontFamily = Jakarta, fontSize = 16.sp, fontWeight = FontWeight.W400),
    bodyMedium = TextStyle(fontFamily = Jakarta, fontSize = 14.sp, fontWeight = FontWeight.W400),
    bodySmall = TextStyle(fontFamily = Jakarta, fontSize = 12.sp, fontWeight = FontWeight.W400),
    labelLarge = TextStyle(fontFamily = Jakarta, fontSize = 14.sp, fontWeight = FontWeight.W600),
    labelMedium = TextStyle(fontFamily = Jakarta, fontSize = 12.sp, fontWeight = FontWeight.W600),
    labelSmall = TextStyle(fontFamily = Jakarta, fontSize = 11.sp, fontWeight = FontWeight.W600, letterSpacing = 0.5.sp)
)

// Expressive corners: noticeably rounder than the baseline so cards and sheets feel soft
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

/**
 * Spacing tokens so every screen uses the same rhythm.
 */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

/**
 * App theme. [palette] picks the colours: Material You wallpaper colours on Android 12+ (violet before
 * that), or one of the built-in palettes. Follows the system light/dark setting unless the user picked
 * one in settings, and [pureBlack] turns dark backgrounds true black. Uses the expressive motion
 * scheme, so components spring and overshoot slightly instead of easing.
 * [dynamicColor] = false forces the built-in palette, for screenshots that must look the same everywhere.
 */
@Composable
fun VeltoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    palette: ThemePalette = ThemePalette.WALLPAPER,
    pureBlack: Boolean = false,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val useWallpaper = palette == ThemePalette.WALLPAPER && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = if (useWallpaper) {
        val context = LocalContext.current
        when {
            !dark -> dynamicLightColorScheme(context)
            pureBlack -> dynamicDarkColorScheme(context).pureBlack()
            else -> dynamicDarkColorScheme(context)
        }
    } else {
        paletteColorScheme(palette, dark, pureBlack)
    }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = AppShapes,
        typography = AppTypography,
        content = content
    )
}
