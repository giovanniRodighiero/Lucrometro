# Roadmap

Done
- [x] Compose hello world builds and runs locally on the emulator
- [x] Cronet + Yahoo quote spike for SMH.MI (see docs/research/data-sources.md)

- [x] 15 tickers in sequence plus 10 repeat rounds on the emulator: 15/15 OK each time

Next
1. Verify 1D intraday and long-range (5Y/Max) responses from Yahoo via Cronet.
2. Test on a real phone.
3. App proper, per docs/prd.md: Room (watchlist + last quote), ViewModel, watchlist screen,
   add/remove, details screen with range chips, 1D intraday and touch scrub, in-memory session cache.
