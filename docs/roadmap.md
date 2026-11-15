# Roadmap

Done
- [x] Compose hello world builds and runs locally on the emulator
- [x] Cronet + Yahoo quote spike for SMH.MI (see docs/research/data-sources.md)

Next
1. Fetch the full ticker list in sequence; check for rate limiting or blocking.
2. Weekday market-hours run: compare `.MI` quote time to the clock (intraday delay).
3. Test on a real phone.
4. App proper: Room watchlist, ViewModel, WorkManager checks a few times per day, notifications.
5. Check Alpha Vantage and Stooq as fallback `QuoteSource` implementations.
