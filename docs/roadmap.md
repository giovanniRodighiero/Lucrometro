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
- [x] Details screen built (decisions 0024-0029): top bar with the market status, quote header, readout, chart
      (1D and 5D intraday, 1M to Max), range bar, touch scrub, session cache, failed/empty/loading states.
- [x] Market times in the exchange's clock, fetch times in the phone's clock; exchange zone saved with the
      quote, Room schema v3 (decision 0031).

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
1. Remaining Yahoo checks for the details screen: what the 1D range and `currentTradingPeriod` return on a weekend
   and an exchange holiday (the market status reads Yahoo's period and skips weekends itself, decision 0027), and
   whether the ~15 minute `.MI` delay is constant. Also what 1D returns before the first trade of the day. One request at a time (CLAUDE.md).
   Done 2026-11-18: 1D intraday, long ranges, `adjclose`, previous close, quote time, trading period, delay
   (docs/research/data-sources.md).
2. Test on a real phone, including the details screen with TalkBack (the scrub actions, decision 0029) and the drawing in
   light and dark.
3. Details screen, open points: the loading state is a plain progress indicator and the empty 1D a plain card (not in Doop);
   check what a weekend and a holiday look like (item 1); the markets' holidays are not known (decision 0027).
4. Personal position (decision 0033): designed in Doop (`Details · position ...` frames, `PositionCard`), not built.
   First `/doop-sync export`, then: Room v4 with dated entries, the card, the add/edit sheet, the average line on the
   chart, the chart at 292dp. The watchlist line is designed but undecided ("Exploration · Position" page).

Later (v2 candidates, not in v1 scope)
- Full buy log per ticker (date, quantity, price) behind the position card's footer (decision 0033).
- A private note per ticker.
- More details info: 52-week and day range, distance from the all-time high, multi-period performance with YTD,
  dividends on the chart, delay notice.
