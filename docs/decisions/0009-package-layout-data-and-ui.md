# 0009: Package layout: data and ui under com.lucrometro

Status: superseded by 0030 (2026-11-18)

## Context
The app is a single `:app` module (0002). The spike put the network code in `data/` and everything
else in the root package. Before the real screens arrive, the folder layout should follow the
standard Android conventions, so agents and Android Studio find things where they expect them.

## Decision
- Root package `com.lucrometro`: `MainActivity` only.
- `data/`: `QuoteSource`, its providers (Yahoo via Cronet) and storage (Room, later). Nothing
  outside `data/` knows about Yahoo or Cronet.
- `ui/theme/`: `MaterialTheme` colors, typography, shapes and the semantic gain/loss tokens.
- `ui/<screen>/` (for example `ui/watchlist/`, `ui/details/`): one folder per screen, holding its
  composables and ViewModel. Created when the screen is built, not before.
- Unit tests in `app/src/test/java/com/lucrometro/`, instrumented tests in `app/src/androidTest/`.
- User-visible text lives in `res/values/strings.xml`, never inline in composables.
- Launcher icon in `mipmap-anydpi-v26` (adaptive icons need API 26; minSdk is 26). Other drawables
  in `drawable/`, named with an `ic_` prefix for icons.

## Consequences
- No `domain/` layer yet. Add it only if use cases start to repeat across screens.
- Screens stay in `ui/`; a screen never imports from `data/` except through its ViewModel.
- `values/strings.xml` is the only text source, so localisation stays possible later.
