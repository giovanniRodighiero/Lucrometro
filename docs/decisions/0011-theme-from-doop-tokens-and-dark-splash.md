# 0011: App theme from the Doop tokens; dark splash on API 31+

Status: accepted (2026-11-17)

## Context
Reviewing decision 0010 raised three theme gaps: a light-only window theme (`Theme.Lucrometro` was the
framework Light theme), a cream splash that flashes in dark mode, and no `ui/theme/` yet. The Doop canvas
already holds a full light and dark token set (snapshot in `docs/design/doop/`, decision 0003 and
`docs/design/native-look-and-feel.md` require following the system theme with dynamic color).

## Decision
- `ui/theme/` (`Color`, `Type`, `Shape`, `Theme`) is built from `docs/design/doop/tokens.json`.
  `LucrometroTheme` follows the system light/dark setting and uses Material You dynamic color on
  Android 12+; below that it uses the static Doop schemes. Type and shape tokens equal the Material 3
  baseline and are pinned explicitly; price text uses tabular figures.
- Gain/loss colors (decision 0007) are a `GainLossColors` holder read as `MaterialTheme.gainLoss`.
  They keep the Doop values under dynamic color so green and red keep their meaning.
- Window theme: `Theme.Lucrometro` is the framework Light theme in `values/` and the dark Material theme
  in `values-night/` (the framework has no DayNight Material theme below API 29). `window_background` is
  the Doop `surface` token in both modes.
- Splash: light mode is unchanged (cream). In dark mode on API 31+ (`values-night-v31`) the ground is the
  dark `surface` and the cream icon background shows as a disc behind the artwork
  (`Theme.SplashScreen.IconBackground`; the plain parent ignores the icon background). Below API 31 dark
  mode keeps the cream splash, because the icon's dark-green strokes would vanish on a dark ground there.

## Consequences
- Supersedes the dark-mode consequence of 0010 ("splash stays cream in dark mode"), except below API 31.
- Edit colors in Doop first, re-export, then update `Color.kt`. Entries marked "derived" in `Color.kt` are
  not Doop tokens (background, inverse*, surfaceVariant, surfaceBright/Dim, surfaceTint).
- Not done yet: edge-to-edge and transparent system bars (the status bar is still the framework default),
  and `setKeepOnScreenCondition` for the splash. Both belong to the watchlist screen work.
- The five unused `--color-icon-*` tokens (leftovers of rejected icon concepts) were deleted from Doop and
  are not in `Color.kt`.
