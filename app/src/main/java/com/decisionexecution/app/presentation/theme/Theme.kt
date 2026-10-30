package com.decisionexecution.app.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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

// Purple fallback for devices without Material You (before Android 12); matches the launcher icon.
private val LightColors = lightColorScheme(
    primary = Color(0xFF6B4EE6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6DEFF),
    onPrimaryContainer = Color(0xFF21005E),
    secondaryContainer = Color(0xFFE6DEFF),
    onSecondaryContainer = Color(0xFF21005E),
    // Break mode gets its own calm tone so it never reads as focus time
    tertiary = Color(0xFF2E7D6B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC8F0E3),
    onTertiaryContainer = Color(0xFF00201A),
    background = Color(0xFFFCFAFF),
    onBackground = Color(0xFF1C1B20),
    surface = Color(0xFFFCFAFF),
    onSurface = Color(0xFF1C1B20),
    surfaceVariant = Color(0xFFE7E0F0),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF7A757F),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFCBBEFF),
    onPrimary = Color(0xFF35168F),
    primaryContainer = Color(0xFF4D33C2),
    onPrimaryContainer = Color(0xFFE6DEFF),
    secondaryContainer = Color(0xFF4A3F75),
    onSecondaryContainer = Color(0xFFE6DEFF),
    tertiary = Color(0xFF7FD6BF),
    onTertiary = Color(0xFF00382D),
    tertiaryContainer = Color(0xFF005143),
    onTertiaryContainer = Color(0xFFA5F2DA),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF2B2832),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF948F99),
    error = Color(0xFFFFB4AB)
)

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
 * App theme. Uses Material You wallpaper colours on Android 12+ and the purple palette before that.
 * Follows the system light/dark setting unless the user picked one in settings. Uses the expressive
 * motion scheme, so components spring and overshoot slightly instead of easing.
 */
@Composable
fun VeltoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkColors
        else -> LightColors
    }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = AppShapes,
        typography = AppTypography,
        content = content
    )
}
