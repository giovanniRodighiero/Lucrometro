# Changelog

All notable changes to Lucrometro are documented in this file.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions match the app's `versionName`.

## [0.2] - 2026-11-18

### Added
- Details screen: tap a ticker to see its price, day change and chart, with the market status in the top bar
  (open with its closing time, or closed with the next opening time).
- Chart ranges: 1D (the default) and 5D intraday, then 1M up to the full history, switched from a range bar.
- Touch scrub on the chart: drag along the line to see the date, price and change at that point.
  TalkBack users can step through the points with "Previous point" and "Next point".
- The 1D chart says "No trades yet" before the first trade of the day.

### Changed
- Opening a ticker starts on the 1D chart and loads fresh data. The header and the watchlist row take the price
  from the chart, so they show the same quote.
- Times follow the exchange's clock (Milan), wherever the phone is set.
- Charts are kept for a short time (2 minutes for intraday ranges, 1 hour for the others), so switching ranges is quick.
- The add sheet no longer opens the keyboard by itself.

### Known limitations
- Prices are delayed by about 15 minutes, as Yahoo reports them. The app does not show a delay notice yet.
- Exchange holidays are not known: on a holiday the market status can read "opens tomorrow" until that day has passed.
- Yahoo Finance is unofficial and can stop working. When a price can't be fetched, the app shows the last saved quote
  as stale, or an error on the chart.
- Tested on the Android emulator (API 33) only. Not yet tested on a physical phone, or with TalkBack on the chart.

## [0.1] - 2026-11-18

### Added
- Watchlist of ETFs and stocks listed on Borsa Italiana, added by ticker code (for example SMH.MI or ENI.MI).
- Price and day-change pill on each row, with the time of the last update.
- Pull to refresh, and a refresh when the app opens.
- Offline use: the last quotes are saved and shown as stale when they can't be refreshed.
- Swipe to remove a ticker, with an undo snackbar. TalkBack users also get a remove action.
- Italian translation, and a per-app language setting on Android 13 and later.
- App icon and splash screen.

### Known limitations
- No details screen, charts or price history yet.
- Tested on the Android emulator (API 33) only. Not yet tested on a physical phone.
