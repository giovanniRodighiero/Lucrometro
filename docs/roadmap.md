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
- [x] Personal position built (decisions 0033, 0034, 0036): Room v4 with dated entries (removed and restored with the
      ticker), `PositionCard`, add/edit sheet with the date picker, average line or edge marker and last-buy diamond on
      the chart, chart at 292dp. Checked on the emulator in English and Italian. The watchlist line is not planned.
- [x] Auto-refresh on the details screen built (decisions 0035, 0037): market marker and sheet, sync icon, 15 s polling (decision 0038)
      of the 1D chart, halo. Checked on the emulator: polls update header and chart, stops on leaving, disabled when closed.

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
2. Real phone: 0.3 works (2026-11-20). Still to test there: TalkBack, including the details screen (the scrub
   actions, decision 0029; the position card and sheet, decision 0033).
3. Details screen, open points: the loading state is a plain progress indicator and the empty 1D a plain card (not in Doop);
   check what a weekend and a holiday look like (item 1); the markets' holidays are not known (decision 0027).
4. The ~15 minute `.MI` delay is not stated in the app: Yahoo's chart `meta` has no delay field (decision 0037). Find
   a source per exchange, or state it for `.MI` only. Still to verify: whether Yahoo has a push channel (WebSocket)
   for `.MI`; with the delay it may add nothing.

Portfolio track (decision 0040): one branch per milestone, from main
- M1, average only: `PositionCard`, sheets and chart without the date, the diamond and the shares. Designed in Doop
  2026-11-22, built and merged into main 2026-11-22 (Room v5 keeps the latest average per ticker). Ships in the next release.
- M2, trade log and private view: the wallet switch, private position card, last buy on the chart, per-ticker
  trades screen, add/edit sheet, per-ticker CSV import (decisions 0041, 0042). Designed in Doop 2026-11-22, built
  and merged into main 2026-11-23 (Room v6, Navigation 3: decisions 0044, 0045). Ships in the next release. Checked on the emulator
  2026-11-23 (edit, sold out, import errors, rotation, process death, dark, Italian, 1D/5D last buy); untested:
  TalkBack on the new screens, the picker with files from other apps (Drive).
- M3, realized gains (decision 0043): gross realized gain per sell and per ticker, sold-out tickers kept with no
  shares (card hidden in public). Designed in Doop 2026-11-23 (decision 0047: compact private card, realized badge on
  sell rows, sold-out card and summary, sell sheet preview), snapshotted on branch `realized-gains`; to build.
- M4, portfolio overview: totals and allocation across tickers.

Later (v2 candidates, not in v1 scope)
- A private note per ticker.
- Trade fees: an optional `fees` column in the trade CSV and the sheet, so realized gains can match the broker
  (0041, 0043).
- Estimated net realized gains after Italian capital gains tax (26%, 12.5% on government bonds, losses offset) (0043).
- Ask for the fingerprint or phone PIN before switching to private mode (public/private mode starts as a plain tap; reconsider once the idea is validated).
- More details info: 52-week and day range, distance from the all-time high, multi-period performance with YTD,
  dividends on the chart, delay notice.
