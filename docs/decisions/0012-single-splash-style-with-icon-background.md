# 0012: One splash style with an icon-background disc on every API level

Status: accepted (2026-11-17)

## Context
Decision 0011 split the splash into a base cream style and a `values-night-v31` dark style, on the
belief that the splash icon background was not supported below API 31, so dark mode there kept the
cream ground. Reading the `core-splashscreen` 1.2.0 resources showed that belief was wrong:
`Theme.SplashScreen.IconBackground` also draws the icon-background disc below API 31 (from API 23, in
its window-background drawable). The split also repeated every style item in two files.

## Decision
- A single `Theme.Lucrometro.Splash` in `values/themes.xml`, parent `Theme.SplashScreen.IconBackground`,
  with the cream `ic_launcher_background` as the icon-background disc on every API level.
- The ground comes from a color: `splash_background` is the launcher cream in `values/colors.xml` and the
  dark `window_background` (Doop `surface`) in `values-night/colors.xml`.
- `values-night-v31/` is deleted.

## Consequences
- Supersedes the splash parts of 0010 and 0011: dark mode no longer flashes cream on API 26-30 either,
  and the style is no longer duplicated.
- With the icon-background sizing the artwork is about 17% smaller in light mode than before (240dp
  instead of 288dp). The cream disc is invisible on the cream ground, so it just looks like a smaller icon.
- Checked on an API 33 emulator (light and dark). The API 26-30 path follows the library source and has
  not been run on a device.
