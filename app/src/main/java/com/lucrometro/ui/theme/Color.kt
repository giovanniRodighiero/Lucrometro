package com.lucrometro.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Values come from the Doop canvas tokens (docs/design/doop/tokens.json). Change them there first.
// These are the static fallback schemes (API < 31, or dynamic color off). Entries marked "derived"
// are not Doop tokens: Material needs them, so they are built from neighbouring tokens.

internal val LightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF2F5D7C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD4E4EF),
    onPrimaryContainer = Color(0xFF0E2A3D),
    inversePrimary = Color(0xFF9CC8E4), // derived: dark primary
    secondary = Color(0xFF55616B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E6EC),
    onSecondaryContainer = Color(0xFF1A2630),
    tertiary = Color(0xFF7D5B1C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF5E2BE),
    onTertiaryContainer = Color(0xFF2B1E05),
    background = Color(0xFFFAF8F4), // derived: surface
    onBackground = Color(0xFF1B1C1E), // derived: onSurface
    surface = Color(0xFFFAF8F4),
    onSurface = Color(0xFF1B1C1E),
    surfaceVariant = Color(0xFFE4E2DC), // derived: surfaceContainerHighest
    onSurfaceVariant = Color(0xFF4A4F55),
    surfaceTint = Color(0xFF2F5D7C), // derived: primary
    inverseSurface = Color(0xFF1B1C1E), // derived: onSurface
    inverseOnSurface = Color(0xFFFAF8F4), // derived: surface
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFF74797F),
    outlineVariant = Color(0xFFC4C8CE),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFAF8F4), // derived: surface
    surfaceDim = Color(0xFFEAE8E2), // derived: surfaceContainerHigh
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F3EE),
    surfaceContainer = Color(0xFFF0EEE8),
    surfaceContainerHigh = Color(0xFFEAE8E2),
    surfaceContainerHighest = Color(0xFFE4E2DC),
)

internal val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF9CC8E4),
    onPrimary = Color(0xFF0B2A3C),
    primaryContainer = Color(0xFF1F4560),
    onPrimaryContainer = Color(0xFFCFE6F5),
    inversePrimary = Color(0xFF2F5D7C), // derived: light primary
    secondary = Color(0xFFB9C5CF),
    onSecondary = Color(0xFF23303A),
    secondaryContainer = Color(0xFF3A4853),
    onSecondaryContainer = Color(0xFFD5E1EB),
    tertiary = Color(0xFFE6C48A),
    onTertiary = Color(0xFF3F2D08),
    tertiaryContainer = Color(0xFF5A4220),
    onTertiaryContainer = Color(0xFFF8E2B8),
    background = Color(0xFF121416), // derived: surface
    onBackground = Color(0xFFE3E2E0), // derived: onSurface
    surface = Color(0xFF121416),
    onSurface = Color(0xFFE3E2E0),
    surfaceVariant = Color(0xFF333537), // derived: surfaceContainerHighest
    onSurfaceVariant = Color(0xFFC4C8CE),
    surfaceTint = Color(0xFF9CC8E4), // derived: primary
    inverseSurface = Color(0xFFE3E2E0), // derived: onSurface
    inverseOnSurface = Color(0xFF121416), // derived: surface
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    outline = Color(0xFF8E9298),
    outlineVariant = Color(0xFF44474E),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF282A2C), // derived: surfaceContainerHigh
    surfaceDim = Color(0xFF121416), // derived: surface
    surfaceContainerLowest = Color(0xFF0D0F11),
    surfaceContainerLow = Color(0xFF1A1C1E),
    surfaceContainer = Color(0xFF1E2022),
    surfaceContainerHigh = Color(0xFF282A2C),
    surfaceContainerHighest = Color(0xFF333537),
)

/**
 * Semantic price-change colors (decision 0007). Material has no gain/loss roles, so they live beside
 * the color scheme. They stay the Doop values under dynamic color too: green and red must keep their
 * meaning whatever the wallpaper is. Never rely on color alone: also show the sign and an arrow.
 */
@Immutable
data class GainLossColors(
    val gain: Color,
    val loss: Color,
    val gainContainer: Color,
    val onGainContainer: Color,
    val lossContainer: Color,
    val onLossContainer: Color,
)

internal val LightGainLossColors = GainLossColors(
    gain = Color(0xFF17652E),
    loss = Color(0xFFB3261E),
    gainContainer = Color(0xFFCDEBD3),
    onGainContainer = Color(0xFF0B3D1A),
    lossContainer = Color(0xFFF9DEDC),
    onLossContainer = Color(0xFF410E0B),
)

internal val DarkGainLossColors = GainLossColors(
    gain = Color(0xFF7FD493),
    loss = Color(0xFFF2B8B5),
    gainContainer = Color(0xFF1E4D2B),
    onGainContainer = Color(0xFFB5EBC1),
    lossContainer = Color(0xFF8C1D18),
    onLossContainer = Color(0xFFF9DEDC),
)
