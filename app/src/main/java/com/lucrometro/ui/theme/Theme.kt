package com.lucrometro.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

private val LocalGainLossColors = staticCompositionLocalOf<GainLossColors> {
    error("GainLossColors not provided: wrap the content in LucrometroTheme")
}

/** Semantic gain/loss colors for the current theme: `MaterialTheme.gainLoss.gain` and so on. */
val MaterialTheme.gainLoss: GainLossColors
    @Composable
    @ReadOnlyComposable
    get() = LocalGainLossColors.current

/**
 * App theme. Follows the system light/dark setting (no in-app switch in v1). Uses Material You
 * dynamic color on Android 12+ and the static Doop schemes below that.
 */
@Composable
fun LucrometroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    CompositionLocalProvider(
        LocalGainLossColors provides if (darkTheme) DarkGainLossColors else LightGainLossColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
