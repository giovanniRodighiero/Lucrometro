# Roadmap

Done
- [x] Compose hello world builds and runs locally on the emulator
- [x] Cronet + Yahoo quote spike for SMH.MI (see docs/research/data-sources.md)

- [x] Doop snapshot exported to docs/design/doop/ and the app theme built from its tokens (decision 0011):
      light/dark/dynamic color, gain/loss colors, dark splash on API 31+
- [x] 15 tickers in sequence plus 10 repeat rounds on the emulator: 15/15 OK each time

- [x] App icon designed and added (decision 0008); checked on the API 33 emulator (launcher icon, monochrome
      layer, clip gap). Artwork scaled to fit the safe zone and splash screen added (decision 0010).
- [x] Details screen designed in Doop and snapshotted (`details-*` frames, decision 0023).

- [x] Watchlist rows from the Doop QuoteRow/ChangeBadge: price, day change pill, stale mark, "Updated"
      stamp, pull-to-refresh and refresh on open (decision 0014). Checked on the emulator online, offline
      (stale) and back online, in light and dark.
- [x] Room 3 storage for the watchlist and last quotes (decision 0015). Force-stop and reopen offline:
      saved quotes shown, marked stale.
- [x] Swipe to remove with undo snackbar, TalkBack remove action (decision 0018).
- [x] Swipe-to-remove review fixes: undo order, failed-undo message, removal scope (decision 0019).
- [x] Italian translation and per-app language on Android 13+ (decision 0017).
- [x] Code review fixes (decision 0016): cancellable source lock, session staleness, schema v2 `position`.

Next
0. On a real phone: check the themed icon (Android 13+, Settings > Wallpaper & style > Themed icons)
   and other launcher shapes (squircle, teardrop). The emulator cannot toggle themed icons.
1. Verify 1D intraday and long-range (5Y/Max) responses from Yahoo via Cronet. Also verify that the
   chart endpoint returns `adjclose` for ETFs (VWCE.MI, SWDA.MI) and that the previous session close is
   available for the 1D baseline (decision 0022). Until then, the UI must not say "adjusted".
   For the details header and market status (decision 0023): the quote timestamp (`regularMarketTime`),
   the trading period (`currentTradingPeriod`), how holidays show up, and the `.MI` intraday delay.
2. Test on a real phone.
3. App proper, per docs/prd.md: Room (watchlist + last quote), ViewModel, watchlist screen,
   add/remove, details screen with the range bar, 1D intraday and touch scrub, in-memory session cache.
4. `QuoteSource` gaps for v1: 1D intraday (minute candles), history for 6M, 1Y, 5Y, Max (daily up to
   1Y; weekly/monthly acceptable for 5Y/Max), a "does this ticker exist" check for add-validation.
   Quotes must carry the currency code.
5. Run `/doop-sync port details-1d` for the details screen (decision 0023); check the header at 130% and 200%
   font scale. Not designed yet: loading state, 1D before the first trade of the day, 5Y/Max.

Later (v2 candidates, not in v1 scope)
- Personal position per ticker (quantity, average price, P&L), a private note per ticker.
- More details info: 52-week and day range, distance from the all-time high, multi-period performance with YTD,
  dividends on the chart, delay notice.
