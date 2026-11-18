# Native look and feel (Android, Material 3)

The app must feel like a first-class native Android app, not a web view or a cross-platform port.
Rule: use standard Material 3 components and system behaviors first; custom drawing only where no
component exists (the price chart). When in doubt, follow the guidelines below instead of inventing.
Decision: [0003](../decisions/0003-native-material3-look-and-feel.md).

References (source of truth for the design phase):
- Material 3 spec, components and patterns: https://m3.material.io
- Android design hub (layout, navigation, adaptive): https://developer.android.com/design/ui/mobile
- Material 3 in Compose (theme, color, typography, shapes): https://developer.android.com/develop/ui/compose/designsystems/material3
- Predictive back: https://developer.android.com/develop/ui/compose/system/predictive-back-setup
- Core app quality checklist: https://developer.android.com/quality

Requirements:
- Theming: `MaterialTheme` with `colorScheme`, `typography`, `shapes`; no hard-coded colors or text
  sizes in screens. Dynamic color (Material You) on Android 12+, with a static fallback scheme on
  older devices. Follows system light/dark; no in-app theme switch in v1.
- Gain/loss colors (green/red) come from theme-level semantic tokens that work in light, dark and
  dynamic color, and never rely on color alone (also sign +/- and an arrow or text).
- Components: Material 3 `Scaffold`, top app bar, `LazyColumn` list items, `FloatingActionButton`
  or equivalent for add, a full-width range bar for chart ranges (decision 0023), `Snackbar` for undo, dialog or
  bottom sheet for add-ticker, `PullToRefresh`, standard swipe-to-dismiss.
- System behavior: edge-to-edge with correct insets, predictive back, system back from details
  returns to the list, state survives rotation and process death, system font scaling respected.
- Typography and layout: Material type scale, 8dp/4dp grid, touch targets of at least 48dp.
- Accessibility: content descriptions (incl. a text summary of the chart), TalkBack-friendly order,
  contrast per Material, scrub also reachable without precise touch (e.g. step buttons or
  semantics actions).
- Motion: default Material transitions (shared axis / container transform between list and
  details), no custom web-like animations.
- Tablets/foldables: not designed for in v1, but use window size classes and don't hard-code
  widths so adaptive layouts can come later.
- Fonts: system font (Roboto/device default) unless decided otherwise; numbers use tabular figures
  so prices align.
