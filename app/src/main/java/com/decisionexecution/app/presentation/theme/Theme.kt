package com.decisionexecution.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.decisionexecution.app.domain.model.ThemeMode

// One accent (primary) for primary actions; everything else stays neutral.
// Requirements: 12.2
private val Accent = Color(0xFF3D5AFE)
private val AccentDark = Color(0xFF8C9EFF)
// Break mode gets its own calm tone so it never reads as focus time (Requirement 5.6)
private val BreakTone = Color(0xFF2E7D6B)
private val BreakToneDark = Color(0xFF7FD6BF)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E5FF),
    onPrimaryContainer = Color(0xFF00105C),
    // Chips and tonal buttons use secondaryContainer; keep it on the accent hue
    secondaryContainer = Color(0xFFE0E5FF),
    onSecondaryContainer = Color(0xFF00105C),
    tertiary = BreakTone,
    onTertiary = Color.White,
    background = Color(0xFFFAFAFC),
    onBackground = Color(0xFF1A1B1F),
    surface = Color(0xFFFAFAFC),
    onSurface = Color(0xFF1A1B1F),
    surfaceVariant = Color(0xFFE6E7EC),
    onSurfaceVariant = Color(0xFF45464F),
    outline = Color(0xFF767680),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = Color(0xFF001A8F),
    primaryContainer = Color(0xFF1F35B8),
    onPrimaryContainer = Color(0xFFE0E5FF),
    secondaryContainer = Color(0xFF2A3A8F),
    onSecondaryContainer = Color(0xFFE0E5FF),
    tertiary = BreakToneDark,
    onTertiary = Color(0xFF00382D),
    background = Color(0xFF121316),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF121316),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF2B2C33),
    onSurfaceVariant = Color(0xFFC6C5D0),
    outline = Color(0xFF90909A),
    error = Color(0xFFFFB4AB)
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.Light, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
)

/**
 * Spacing tokens so every screen uses the same rhythm. Requirements: 12.7
 */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

/**
 * App theme. Follows the system unless the user picked Light or Dark in settings.
 * Requirements: 12.1, 12.2, 12.8
 */
@Composable
fun DecisionExecutionTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
