# Roadmap

Done
- [x] Compose hello world builds and runs locally on the emulator
- [x] Cronet + Yahoo quote spike for SMH.MI (see docs/research/data-sources.md)

- [x] Doop snapshot exported to docs/design/doop/ and the app theme built from its tokens (decision 0011):
      light/dark/dynamic color, gain/loss colors, dark splash on API 31+
- [x] 15 tickers in sequence plus 10 repeat rounds on the emulator: 15/15 OK each time

- [x] App icon designed and added (decision 0008); checked on the API 33 emulator (launcher icon, monochrome
      layer, clip gap). Artwork scaled to fit the safe zone and splash screen added (decision 0010).

Next
0. On a real phone: check the themed icon (Android 13+, Settings > Wallpaper & style > Themed icons)
   and other launcher shapes (squircle, teardrop). The emulator cannot toggle themed icons.
1. Verify 1D intraday and long-range (5Y/Max) responses from Yahoo via Cronet.
2. Test on a real phone.
3. App proper, per docs/prd.md: Room (watchlist + last quote), ViewModel, watchlist screen,
   add/remove, details screen with range chips, 1D intraday and touch scrub, in-memory session cache.
4. `QuoteSource` gaps for v1: 1D intraday (minute candles), history for 6M, 1Y, 5Y, Max (daily up to
   1Y; weekly/monthly acceptable for 5Y/Max), a "does this ticker exist" check for add-validation.
   Quotes must carry the currency code.
5. Run `/doop-sync port <frame>` for the watchlist (the export and the theme are done; the theme is not yet
   used by a real screen). Edge-to-edge and system bars come with it.
6. Build the watchlist screen from Doop decision 0007 (minimal rows with tonal pills).
