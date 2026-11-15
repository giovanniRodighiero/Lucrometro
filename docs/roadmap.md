# Roadmap

Done
- [x] Compose hello world builds and runs locally on the emulator
- [x] Cronet + Yahoo quote spike for SMH.MI (see docs/research/data-sources.md)

- [x] 15 tickers in sequence plus 10 repeat rounds on the emulator: 15/15 OK each time

Next
1. Weekday market-hours run: compare `.MI` quote time to the clock (intraday delay).
2. Test on a real phone.
3. App proper: Room watchlist, ViewModel, WorkManager checks a few times per day, notifications.
4. Check Alpha Vantage and Stooq as fallback `QuoteSource` implementations.
