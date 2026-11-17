# Roadmap

Done
- [x] Compose hello world builds and runs locally on the emulator
- [x] Cronet + Yahoo quote spike for SMH.MI (see docs/research/data-sources.md)

- [x] 15 tickers in sequence plus 10 repeat rounds on the emulator: 15/15 OK each time

- [x] App icon designed and added (decision 0008); not yet verified on a device or emulator

Next
0. Check the app icon: normal launcher, themed icon (Android 13+, Settings > Wallpaper & style >
   Themed icons), and a few launcher shapes. The monochrome layer uses a clip path: confirm the
   gap around the note renders.
1. Verify 1D intraday and long-range (5Y/Max) responses from Yahoo via Cronet.
2. Test on a real phone.
3. App proper, per docs/prd.md: Room (watchlist + last quote), ViewModel, watchlist screen,
   add/remove, details screen with range chips, 1D intraday and touch scrub, in-memory session cache.
4. `QuoteSource` gaps for v1: 1D intraday (minute candles), history for 6M, 1Y, 5Y, Max (daily up to
   1Y; weekly/monthly acceptable for 5Y/Max), a "does this ticker exist" check for add-validation.
   Quotes must carry the currency code.
5. Skill to export Doop designs (tokens, guidelines, frames) into `docs/design/` and port them to Compose.
6. Build the watchlist screen from Doop decision 0007 (minimal rows with tonal pills).
