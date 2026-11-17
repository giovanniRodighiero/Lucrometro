# 0008: App icon is a smiling euro banknote over an area chart

Status: accepted (2026-11-17)

## Context
The app needed an icon that is a bit fun, not too serious, and says "money" and "stocks" at a glance.
About 20 proposals were explored on the Doop canvas "Lucrometro" (page "App icon"): gauge, espresso cup
with a chart as steam, tape measure ("metro"), smiling candlesticks, coin, money bag, piggy bank,
banknote with candles, bars, chart window and ticker triangles. Coffee was rejected as unrelated to
the product.

## Decision
A tilted euro banknote with a smiling face in its medallion, in front of a green area chart that rises
to the right, on a cream background (candidate G6). The face is the fun element; the chart is the
stock cue.

Delivered as:
- Adaptive icon (minSdk is 26, so no legacy PNGs or round icon are needed): flat cream background,
  foreground vector, and a monochrome vector for Android 13+ themed icons.
  See `app/src/main/res/{drawable,mipmap-anydpi,values}/ic_launcher*`.
- `docs/brand/app-icon.svg`: full-colour master. `docs/brand/play-store-icon-512.png`: 512x512 Play
  Store image (only needed if the app is published on Google Play).

## Consequences
- Icon colours live in `values/ic_launcher_colors.xml` and are the same values as the Doop tokens
  (`gain`, `gainContainer`, `surface`, plus a cream `ic_launcher_background`). They are not part of the
  app theme: the icon is brand artwork.
- The monochrome layer relies on a clip path with a counter-wound hole to separate the chart from the
  note. If a launcher renders it wrongly, replace the clip with pre-computed geometry.
- No notification or splash icon was made: v1 has no notifications, and Android 12+ reuses the
  launcher icon for the splash screen.
