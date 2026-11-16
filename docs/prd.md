# FinanceApp: Product Requirements (v1)

Status: draft, 2026-11-16. Single user, personal tool.

## Purpose
Check a short, dynamic list of ETFs and Borsa Italiana stocks a few times per day, quickly see how
each is doing, and drill into a chart to see how it moved over time.

## Principles
- Correctness over availability: if data can't be fetched, say so; never show wrong data.
- 100% local: no accounts, no own server, no analytics. All data stays on the device.
- 100% free data source (Yahoo via Cronet behind `QuoteSource`, see decision 0001).
- Fast to glance at: opening the app should answer "how are they doing?" in a second.

## Users and usage
One user (the owner). Opens the app a few times per day. Watchlist size ~10-15 tickers (design for
up to ~50 without redesign).

## v1 scope

### 1. Watchlist (home screen)
- Shows all saved tickers, one row each: ticker code (name if available), last price, day change %
  (green up / red down / neutral flat).
- No sparkline or multi-period % on the row (deliberately minimal).
- Pull-to-refresh. Auto-refresh on app open.
- Tapping a row opens the details screen.
- Shows an "updated HH:mm" stamp. Price is shown in the ticker's own currency (EUR, USD, ...).
- If a fetch fails, the row keeps its last saved quote, clearly marked stale with its timestamp
  ("stale since HH:mm"). If there is no saved quote yet, the row shows "Couldn't fetch". Never show
  wrong or substitute data.

### 2. Add ticker
- A field where the user pastes a ticker code (e.g. `ENI.MI`, `VWCE.MI`). No search/autocomplete.
- Normalize input (trim, uppercase).
- Validate before saving: fetch a quote; if it resolves, save, otherwise show a clear error and do
  not save. Reject duplicates.

### 3. Remove ticker
- Swipe-to-delete or action on the row, with undo (snackbar). Removes its cached data too.

### 4. Details screen
- Header: ticker, last price, day change (abs and %), last updated.
- Price chart (line, area fill). Prices and scrub values are shown in the ticker's own currency.
- Chart data is never persisted. If the fetch fails, the chart area shows "Couldn't fetch" with a
  retry action (no stale chart).
- Range chips: 1D (intraday), 5D, 1M, 3M, 6M, 1Y, 5Y, Max. Default 1M.
- Touch scrub: dragging a finger along the chart shows date and price at that point and the change
  (abs and %) from the start of the selected range. Releasing returns to the range summary.
- Range summary: change % over the selected range.

### 5. Persistence
- Room stores only: the watchlist (ticker codes, order of addition) and the last successful quote
  per ticker (price, day change, currency, fetched-at timestamp). This feeds the stale display on
  the overview.
- Chart history/intraday points are NOT stored in Room.
- Session cache (in memory only, lost when the process dies): chart responses keyed by
  ticker + range. Re-opening the same ticker/range in the same session reuses the response instead
  of refetching. Historical ranges (5D and up) are reused for the whole session; 1D intraday uses a
  short TTL (proposed 2 min) since it changes during market hours. Pull-to-refresh and the retry
  action bypass the cache.

## Out of scope for v1 (parked)
Reorder list, price alerts/notifications, background refresh (WorkManager), holdings/P&L,
search/autocomplete, pinch-zoom and pan on the chart, multiple lists, widgets, currency conversion,
fallback data providers.

## Look and feel (native Android, Material 3)
The app must feel like a first-class native Android app, not a web view or a cross-platform port.
Rule: use standard Material 3 components and system behaviors first; custom drawing only where no
component exists (the price chart). When in doubt, follow the guidelines below instead of inventing.

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
  or equivalent for add, `FilterChip`/`SegmentedButton` for ranges, `Snackbar` for undo, dialog or
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

## Non-functional
- Data provider only touched through `QuoteSource` (nothing outside `data/` knows Yahoo/Cronet).
- Must tolerate provider failures (the unofficial Yahoo endpoint can change or rate-limit): show
  a "couldn't fetch" error state, never wrong data, never crash. No fallback provider.
- Refresh fetches tickers sequentially (stress-tested in docs/research/data-sources.md).
- Switching back to an already-loaded ticker+range is instant (session cache, see Persistence).

## Needed from `QuoteSource` (gap vs. current code)
Current: latest quote and daily history for 5d, 1mo, 3mo. v1 needs 1D intraday (minute-level
candles), history for 6M, 1Y, 5Y, Max
(daily for up to 1Y; weekly/monthly granularity acceptable for 5Y/Max) and a "does this ticker
exist" check usable for add-validation. Quotes must carry the currency code.

## Open questions
- 1D chart when the market is closed or on weekends: show the last trading session.
- Mixed currencies on the list: shown per ticker, no conversion or totals (decided).

## Success criteria
- Add a pasted code, see it on the list with price and day % within a few seconds.
- Remove it with undo.
- Open details, switch ranges, scrub the chart.
- Kill and reopen the app offline: the list still shows last quotes marked stale; opening a chart
  shows "Couldn't fetch".
