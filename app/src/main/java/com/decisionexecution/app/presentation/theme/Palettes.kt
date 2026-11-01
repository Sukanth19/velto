package com.decisionexecution.app.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.decisionexecution.app.domain.model.ThemePalette

/**
 * The accent colours of one palette in one brightness. Surfaces aren't listed: they're the neutral
 * greys tinted slightly towards [primary], so every palette gets a matching background for free.
 * Tertiary is the break-time colour, so it's always a calm tone that doesn't read as focus.
 */
private data class Accents(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color
)

private class PaletteSpec(val light: Accents, val dark: Accents)

private fun c(hex: Long) = Color(hex)

private val Violet = PaletteSpec(
    light = Accents(
        c(0xFF6B4EE6), Color.White, c(0xFFE6DEFF), c(0xFF21005E),
        c(0xFF5E5873), c(0xFFE6DEFF), c(0xFF21005E),
        c(0xFF2E7D6B), Color.White, c(0xFFC8F0E3), c(0xFF00201A)
    ),
    dark = Accents(
        c(0xFFCBBEFF), c(0xFF35168F), c(0xFF4D33C2), c(0xFFE6DEFF),
        c(0xFFCAC1E9), c(0xFF4A3F75), c(0xFFE6DEFF),
        c(0xFF7FD6BF), c(0xFF00382D), c(0xFF005143), c(0xFFA5F2DA)
    )
)

private val Ocean = PaletteSpec(
    light = Accents(
        c(0xFF0061A4), Color.White, c(0xFFD1E4FF), c(0xFF001D36),
        c(0xFF535F70), c(0xFFD7E3F7), c(0xFF101C2B),
        c(0xFF006A60), Color.White, c(0xFF9EF2E4), c(0xFF00201C)
    ),
    dark = Accents(
        c(0xFF9ECAFF), c(0xFF003258), c(0xFF00497D), c(0xFFD1E4FF),
        c(0xFFBBC7DB), c(0xFF3B4858), c(0xFFD7E3F7),
        c(0xFF82D5C8), c(0xFF003731), c(0xFF005048), c(0xFF9EF2E4)
    )
)

private val Forest = PaletteSpec(
    light = Accents(
        c(0xFF2E6A44), Color.White, c(0xFFB1F1C1), c(0xFF00210E),
        c(0xFF4F6352), c(0xFFD2E8D3), c(0xFF0D1F12),
        c(0xFF3A646F), Color.White, c(0xFFBEEAF6), c(0xFF001F26)
    ),
    dark = Accents(
        c(0xFF96D5A6), c(0xFF00391C), c(0xFF12512E), c(0xFFB1F1C1),
        c(0xFFB6CCB8), c(0xFF384B3C), c(0xFFD2E8D3),
        c(0xFFA2CEDA), c(0xFF023640), c(0xFF214C57), c(0xFFBEEAF6)
    )
)

private val Sunset = PaletteSpec(
    light = Accents(
        c(0xFF8F4C00), Color.White, c(0xFFFFDCC2), c(0xFF2E1500),
        c(0xFF735943), c(0xFFFFDCC2), c(0xFF2A1707),
        c(0xFF006874), Color.White, c(0xFF97F0FF), c(0xFF001F24)
    ),
    dark = Accents(
        c(0xFFFFB77C), c(0xFF4D2600), c(0xFF6D3900), c(0xFFFFDCC2),
        c(0xFFE3C0A5), c(0xFF5A422D), c(0xFFFFDCC2),
        c(0xFF4FD8EB), c(0xFF00363D), c(0xFF004F58), c(0xFF97F0FF)
    )
)

private val Rose = PaletteSpec(
    light = Accents(
        c(0xFF984061), Color.White, c(0xFFFFD9E2), c(0xFF3E001D),
        c(0xFF74565F), c(0xFFFFD9E2), c(0xFF2B151C),
        c(0xFF006A65), Color.White, c(0xFF70F7EE), c(0xFF00201E)
    ),
    dark = Accents(
        c(0xFFFFB0C8), c(0xFF5E1133), c(0xFF7B2949), c(0xFFFFD9E2),
        c(0xFFE2BDC6), c(0xFF5A3F47), c(0xFFFFD9E2),
        c(0xFF4FDAD1), c(0xFF003735), c(0xFF00504C), c(0xFF70F7EE)
    )
)

private val Mono = PaletteSpec(
    light = Accents(
        c(0xFF2B2D31), Color.White, c(0xFFE2E2E6), c(0xFF1A1C1F),
        c(0xFF5C5E64), c(0xFFE2E2E6), c(0xFF1A1C1F),
        c(0xFF4A6360), Color.White, c(0xFFCCE8E3), c(0xFF051F1D)
    ),
    dark = Accents(
        c(0xFFE3E3E6), c(0xFF2B2D31), c(0xFF45474C), c(0xFFE3E3E6),
        c(0xFFC4C6CC), c(0xFF44474C), c(0xFFE0E2E8),
        c(0xFFB0CCC7), c(0xFF1C3532), c(0xFF334B48), c(0xFFCCE8E3)
    )
)

private fun ThemePalette.spec(): PaletteSpec = when (this) {
    // Wallpaper colours are handled by the caller; this is the fallback before Android 12
    ThemePalette.WALLPAPER, ThemePalette.VIOLET -> Violet
    ThemePalette.OCEAN -> Ocean
    ThemePalette.FOREST -> Forest
    ThemePalette.SUNSET -> Sunset
    ThemePalette.ROSE -> Rose
    ThemePalette.MONO -> Mono
}

/** Name shown under the swatch in settings */
val ThemePalette.label: String
    get() = when (this) {
        ThemePalette.WALLPAPER -> "Wallpaper"
        ThemePalette.VIOLET -> "Violet"
        ThemePalette.OCEAN -> "Ocean"
        ThemePalette.FOREST -> "Forest"
        ThemePalette.SUNSET -> "Sunset"
        ThemePalette.ROSE -> "Rose"
        ThemePalette.MONO -> "Graphite"
    }

/** The three colours a settings swatch shows: primary, its container and the break colour */
fun ThemePalette.swatch(dark: Boolean): List<Color> {
    val a = if (dark) spec().dark else spec().light
    return listOf(a.primary, a.primaryContainer, a.tertiary)
}

private fun tint(base: Long, tint: Color, amount: Float) = lerp(Color(base), tint, amount)

fun paletteColorScheme(palette: ThemePalette, dark: Boolean, pureBlack: Boolean): ColorScheme {
    val spec = palette.spec()
    return if (dark) darkScheme(spec.dark, spec.light.primary, pureBlack) else lightScheme(spec.light, spec.dark.primary)
}

private fun lightScheme(a: Accents, inversePrimary: Color): ColorScheme {
    val t = a.primary
    val surface = tint(0xFFFFFFFF, t, 0.03f)
    return lightColorScheme(
        primary = a.primary,
        onPrimary = a.onPrimary,
        primaryContainer = a.primaryContainer,
        onPrimaryContainer = a.onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = a.secondary,
        onSecondary = Color.White,
        secondaryContainer = a.secondaryContainer,
        onSecondaryContainer = a.onSecondaryContainer,
        tertiary = a.tertiary,
        onTertiary = a.onTertiary,
        tertiaryContainer = a.tertiaryContainer,
        onTertiaryContainer = a.onTertiaryContainer,
        background = surface,
        onBackground = tint(0xFF1B1B1F, t, 0.08f),
        surface = surface,
        onSurface = tint(0xFF1B1B1F, t, 0.08f),
        surfaceVariant = tint(0xFFE4E4E8, t, 0.12f),
        onSurfaceVariant = tint(0xFF46464F, t, 0.12f),
        surfaceTint = a.primary,
        inverseSurface = tint(0xFF303034, t, 0.06f),
        inverseOnSurface = tint(0xFFF2F0F4, t, 0.04f),
        outline = tint(0xFF77777F, t, 0.12f),
        outlineVariant = tint(0xFFC7C7CF, t, 0.10f),
        error = Color(0xFFBA1A1A),
        surfaceBright = surface,
        surfaceDim = tint(0xFFDCDBDF, t, 0.06f),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = tint(0xFFF8F7FA, t, 0.04f),
        surfaceContainer = tint(0xFFF2F1F5, t, 0.05f),
        surfaceContainerHigh = tint(0xFFECEBEF, t, 0.06f),
        surfaceContainerHighest = tint(0xFFE6E5E9, t, 0.07f)
    )
}

private fun darkScheme(a: Accents, inversePrimary: Color, pureBlack: Boolean): ColorScheme {
    val t = a.primary
    val surface = if (pureBlack) Color.Black else tint(0xFF121316, t, 0.04f)
    return darkColorScheme(
        primary = a.primary,
        onPrimary = a.onPrimary,
        primaryContainer = a.primaryContainer,
        onPrimaryContainer = a.onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = a.secondary,
        onSecondary = tint(0xFF2A2A30, t, 0.15f),
        secondaryContainer = a.secondaryContainer,
        onSecondaryContainer = a.onSecondaryContainer,
        tertiary = a.tertiary,
        onTertiary = a.onTertiary,
        tertiaryContainer = a.tertiaryContainer,
        onTertiaryContainer = a.onTertiaryContainer,
        background = surface,
        onBackground = tint(0xFFE4E2E6, t, 0.05f),
        surface = surface,
        onSurface = tint(0xFFE4E2E6, t, 0.05f),
        surfaceVariant = tint(0xFF46464C, t, 0.12f),
        onSurfaceVariant = tint(0xFFC7C6CF, t, 0.08f),
        surfaceTint = a.primary,
        inverseSurface = tint(0xFFE4E2E6, t, 0.05f),
        inverseOnSurface = tint(0xFF303034, t, 0.06f),
        outline = tint(0xFF909099, t, 0.10f),
        outlineVariant = tint(0xFF46464F, t, 0.10f),
        error = Color(0xFFFFB4AB),
        surfaceBright = tint(0xFF38393D, t, 0.06f),
        surfaceDim = surface,
        // Pure black keeps cards a step above the background so they don't disappear
        surfaceContainerLowest = if (pureBlack) Color.Black else tint(0xFF0D0E11, t, 0.03f),
        surfaceContainerLow = tint(if (pureBlack) 0xFF0E0E10 else 0xFF1A1B1E, t, 0.05f),
        surfaceContainer = tint(if (pureBlack) 0xFF141416 else 0xFF1E1F23, t, 0.06f),
        surfaceContainerHigh = tint(if (pureBlack) 0xFF1C1C1F else 0xFF282A2D, t, 0.07f),
        surfaceContainerHighest = tint(if (pureBlack) 0xFF26262A else 0xFF333539, t, 0.08f)
    )
}

/** Pure black version of a dark scheme that came from somewhere else (Material You wallpaper colours) */
fun ColorScheme.pureBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = lerp(surfaceContainerLow, Color.Black, 0.5f),
    surfaceContainer = lerp(surfaceContainer, Color.Black, 0.4f),
    surfaceContainerHigh = lerp(surfaceContainerHigh, Color.Black, 0.35f),
    surfaceContainerHighest = lerp(surfaceContainerHighest, Color.Black, 0.3f)
)
