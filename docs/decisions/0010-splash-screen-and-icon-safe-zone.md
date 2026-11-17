# 0010: Splash screen via core-splashscreen; icon artwork scaled to 92%

Status: accepted (2026-11-17)

## Context
Checking the icon from 0008 on an API 33 emulator showed it rendering well in the launcher, but the
artwork reached about 35.6 units from the centre (the chart's end dot) against the 33-unit adaptive
safe zone (66dp of 108dp). Nothing clipped in a circular mask, but a launcher with a larger mask could.
Android 12+ builds a splash screen from the launcher icon automatically; API 26-30 (minSdk is 26)
shows a blank window until the first frame.

## Decision
- Icon: the whole artwork (foreground and monochrome vectors, `docs/brand/app-icon.svg`, the Doop
  frames and the Play Store PNG) is scaled to 92% about the centre (54,54). Farthest point is now about
  32.8. The cream background layer is unchanged.
- Splash: `androidx.core:core-splashscreen` 1.2.0. `Theme.Lucrometro.Splash` (in `values/themes.xml`)
  is the launch theme of `MainActivity`: cream `ic_launcher_background` ground,
  `ic_launcher_foreground` as the animated icon, `postSplashScreenTheme` = `Theme.Lucrometro`.
  `MainActivity` calls `installSplashScreen()` before `super.onCreate`.

## Consequences
- The splash looks the same on API 26-36. It stays cream in dark mode (brand artwork, like the icon),
  so a dark-mode user sees a brief light flash. If that bothers, add a `values-night` override with a
  dark ground and `windowSplashScreenIconBackgroundColor` = cream (API 31+ only; the icon's dark-green
  strokes would be hard to see on a dark ground below API 31).
- The splash is held only until the first frame. Add `setKeepOnScreenCondition` once the watchlist
  loads data, if a longer hold is wanted.
- Any future change to the icon geometry must keep the artwork inside radius 33.
- Themed icons (Android 13+) and non-circular launcher masks are still untested on a real device.
