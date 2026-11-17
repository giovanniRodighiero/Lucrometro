package com.lucrometro.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// System font (Roboto on Android). Sizes, line heights and weights are the Doop type tokens
// (docs/design/doop/tokens.json). Letter spacing is 0, as in the Doop frames; the Material 3 baseline
// adds 0.1-0.5sp to the body, label and medium title styles, so text is slightly tighter than the
// Material defaults. Styles not listed here (display/headline large and medium, titleSmall,
// labelSmall) keep the Material defaults: no screen uses them. Styles that can show prices or
// changes use tabular figures ("tnum") so digits align.
private const val TABULAR = "tnum"

internal val AppTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Normal, fontFeatureSettings = TABULAR,
    ),
    headlineSmall = TextStyle(
        fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Normal, fontFeatureSettings = TABULAR,
    ),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Normal),
    titleMedium = TextStyle(
        fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal, fontFeatureSettings = TABULAR,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, fontFeatureSettings = TABULAR,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal, fontFeatureSettings = TABULAR,
    ),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)
