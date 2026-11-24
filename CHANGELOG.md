# Changelog

All notable changes to Lucrometro are documented in this file.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions match the app's `versionName`.

## [0.4] - 2026-11-24

### Added
- Public and private view: a wallet button in every top bar switches between them with one tap. Public shows only
  prices and percentages; private adds your shares, values and gains in euros. The app always starts public.
- Trades: in private, each ticker's position card opens its trades. Add, edit or delete buys and sells (with Undo);
  the app keeps the shares from ever going below zero.
- CSV import: load a ticker's trades from a file in the app's own format (ticker, side, date, time, quantity, price,
  id). You see what will be added before anything is saved; rows with an id already saved are skipped, so the
  same file can be imported again.
- Private position card: average price, shares and value, the change since you bought, and the realized gains from
  your sells. Your last buy shows on the chart as a diamond.
- Realized gains (gross, before fees and taxes): on each sell, on the position card, and kept after a ticker is sold
  out.
- Portfolio tab, next to the Watchlist in a new bottom bar:
  - the total value, today's change and the change since you bought, and what you invested;
  - growth by year, which doesn't count new money as growth; tap a year to see each holding's market return;
  - composition by value, and the tickers not counted (average price only, or not in euros);
  - the realized gains of all years.
  The public view shows the percentages only.

### Changed
- The average price you type is now the only thing on the public position card. Updating from 0.3 keeps each
  ticker's latest average; the shares and the last-buy date typed in 0.3 are dropped (add your trades instead).
- Once a ticker has trades, its average is computed from them, as the broker does, without fees.
- "Since avg" is now "Since you bought".
- Change amounts of 10,000 or more are shown without decimals, so a badge stays on one line.
- Moving between the Watchlist and Portfolio tabs fades between them.
- The app's data is no longer included in Android's cloud backup: everything stays on the phone.

### Fixed
- A refresh stops at the first network failure instead of waiting for every other ticker to time out.

### Known limitations
- The portfolio counts tickers in euros only; others are listed as not counted. There is no currency conversion.
- Stock splits are not handled: a split after a trade makes the years before it wrong.
- This year's market return per holding can miss dividends paid since the year-end prices were fetched.
- The first opening of the Portfolio tab fetches each ticker's price history, about 5 seconds per ticker.
- The private view has no fingerprint or PIN yet.
- A typed average can't be removed on its own, except by removing the ticker.
- Exchange holidays are not known, and Yahoo Finance is unofficial and can stop working.
- Not yet tested with TalkBack on the new screens.

## [0.3] - 2026-11-20

### Added
- Your position: on a ticker's details, add the average price your broker shows, and optionally the shares and the date
  of your last buy. A card under the chart shows the average, the change since it and, with shares, the value.
  Each save is kept; the latest is shown. The data stays on the phone.
- The chart shows your average price as a dotted line, and your last buy as a diamond when it falls inside the range.
- Auto-refresh on the details screen: tap the sync button while the market is open to check the price every 15 seconds.
  It stops when you leave the screen, when the market closes, or when a check fails.
- Market marker in the top bar ("Open" or "Closed"). Tap it for the exchange's hours, the time of the last quote,
  the delay of the prices and the auto-refresh state.

### Changed
- The market status adds the phone's clock in parentheses when it differs from the exchange's,
  for example "Closed · opens 09:30 EST (15:30)".
- Coming back from a ticker's details no longer reloads the whole list. The list refreshes when the app is opened or
  brought back.

### Fixed
- When Yahoo answers "too many requests", a refresh stops at once instead of asking for every other ticker.
  The remaining tickers show their last saved quote as stale.

### Known limitations
- Exchange holidays are not known: on a holiday the market status can read "opens tomorrow" until that day has passed.
- Yahoo Finance is unofficial and can stop working. When a price can't be fetched, the app shows the last saved quote
  as stale, or an error on the chart.
- A position can be edited but not removed, except by removing the ticker.
- Tested on the Android emulator (API 33) and on a phone. Not yet tested with TalkBack.

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
