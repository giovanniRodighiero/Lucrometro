# Lucrometro: Product Requirements (v1)

Status: final (v1), 2026-11-17. Single user, personal tool.

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
- Pull-to-refresh. Auto-refresh on app open. Tickers are fetched one at a time.
- Tapping a row opens the details screen.
- Shows an "updated HH:mm" stamp. Price is shown in the ticker's own currency (EUR, USD, ...), with
  no conversion or totals across currencies.
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
- If the fetch fails, the chart area shows "Couldn't fetch" with a retry action (no stale chart).
- Range chips: 1D (intraday), 5D, 1M, 3M, 6M, 1Y, 5Y, Max. Default 1M. 1D shows the last trading
  session when the market is closed or on weekends.
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

## Look and feel
The app feels like a native Android app. Rules and references: [docs/design/native-look-and-feel.md](design/native-look-and-feel.md).

## Success criteria
- Add a pasted code, see it on the list with price and day % within a few seconds.
- Remove it with undo.
- Open details, switch ranges, scrub the chart.
- Kill and reopen the app offline: the list still shows last quotes marked stale; opening a chart
  shows "Couldn't fetch".
