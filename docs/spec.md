# Lucrometro: how the app works

Up to date with: **0.4** (decisions 0001-0053). This is the current state; read it before the decisions.
The decision numbers in brackets point to the reasoning and the options that were turned down. The starting point
was [prd.md](prd.md) (v1); where they disagree, this file wins.

## Principles
- **Correct over available.** When data can't be fetched, say so and show the last saved value as stale.
  Never show wrong or substitute data.
- **All local.** No accounts, no own server, no analytics, no Android backup. Everything stays on the phone; a new
  phone starts empty and the trades are imported again from the broker's CSV (0046).
- **Free data.** Yahoo Finance's unofficial endpoints, behind `QuoteSource` so the provider can be swapped (0001).
- **Native.** Material 3 components and system behaviour; custom drawing only for the chart (0003).
- **Gentle on Yahoo.** One request at a time across the whole app, and a 429 always stops, never retries.

## Navigation
- Two top-level tabs in a Material 3 navigation bar: **Watchlist** and **Portfolio**, in public and private alike
  (0049, 0051). One bar sits under both tabs and stays still; switching tabs is a Material fade-through (out in 90 ms,
  in over 210 ms with a slight zoom). Back on the Portfolio tab goes to the Watchlist first.
- Details, trades and the year detail are pushed screens without the bar, on a Navigation 3 back stack owned by
  `MainActivity`, with its default fade (0044). The back stack and the selected tab survive rotation and process death.

## Public and private view
- A **wallet** button in every top bar switches the view with one tap, no authentication (0040, 0042). Outlined in
  public, filled on tertiary container in private. The app starts public on every launch; the view survives rotation
  and moving between screens, not a restart (0045).
- **Public** shows only relative figures: prices, charts, the typed or computed average and changes in %. **Private**
  adds shares, values, € gains and losses, the trades, the last buy on the chart and the Portfolio's € figures.

## Watchlist (home screen)
- One row per ticker in the order it was added: code, name, price in the ticker's own currency, day change as a
  tonal pill (gain, loss or flat; flat when the rounded value is 0.00%). Plain rows, small FAB, no summary card (0007).
- Day change is measured from the previous session's close (0014).
- An "Updated" stamp shows the newest fetch time, in the phone's clock (0016, 0031).
- **Refresh**: when the app is opened or brought back (skipped within 60 s of the last refresh), and by
  pull-to-refresh, which always fetches. Coming back from a ticker's details is not a refresh. Tickers are fetched one
  at a time. The first 429 or network failure (timeout, offline) stops the refresh and marks the remaining tickers
  stale, so one dead connection doesn't make every ticker wait its own 20 s timeout. (0014, 0016, audits after 0.3)
- **Stale**: a quote is current only if it was fetched in this app session. Otherwise, or when its fetch failed, the
  row keeps it and shows "Stale since <time>" (0016).
- **Add**: a bottom sheet where the code is pasted (no search). Input is trimmed and upper-cased, the format checked,
  duplicates rejected, then a live quote is fetched: the ticker is saved only if it resolves. Errors tell a wrong code
  from a network problem. Closing the sheet cancels the check (0013).
- **Remove**: swipe from the end, or the "Remove" TalkBack action. It is deleted at once together with its saved
  quote, average and trades. A snackbar offers Undo, which puts it back in its old place. Only the latest removal can be
  undone (0018, 0019).

## Details screen
Opened by tapping a row, or a ticker on the Portfolio tab. Each opening starts on 1D and reloads (0029). Layout from
top to bottom (0023, 0035):

- **Top bar**: back, ticker code, the market marker ("● Open" / "○ Closed", never red), the sync button and the
  wallet. The marker and the sync button are hidden when the hours are unknown or the chart failed.
- **Market sheet** (tap the marker): exchange name, full status ("Closed · opens 09:30 EST (15:30)"), trading hours,
  last quote time and the auto-refresh state. A delay row shows only if Yahoo reports one, which it does not today
  (0035, 0037).
- **Quote header**: name, price, day change pill, and the quote's own time ("Today, 14:32", or the session day when
  closed). If the saved quote is stale, it says "Stale since <time>" instead (0023).
- **Readout** above the chart: 1D shows the previous close; other ranges show "From <price>" and the range change pill.
  While scrubbing it shows the point's time, price and change from the range start (0023, 0028).
- **Chart**: line and area, coloured by the range's change; gridlines at round prices; drawn on a Canvas, no chart
  library (0025). 1D spans the whole session with the previous close as a dashed baseline, so the line stops at the
  last trade (0027). 5D spaces its bars evenly so nights and weekends leave no holes (0027).
- **Ranges**: 1D (5-minute bars) and 5D (30-minute bars) use plain closes; 1M to 1Y daily, 5Y weekly, Max monthly,
  all on adjusted closes so dividends don't look like drops (0022, 0025, 0026, 0027). The range bar is eight equal
  48dp cells, not chips or tabs (0023).
- **Scrub**: a horizontal drag shows the nearest point; vertical drags scroll the page. TalkBack gets "Previous
  point", "Next point" and "Back to latest" actions (0028, 0029).
- **Your position** card under the range bar (below).
- **Failure**: the chart area shows "Couldn't fetch" with Retry, never a stale chart. An empty 1D before the first
  trade shows "No trades yet" (0022, 0027).
- A fresh 1D response also updates the ticker's saved quote, so the header and the watchlist row follow the chart
  (0029).

## Market status
- Hours come from the latest chart response: the current session and the exchange's time zone. Before or after the
  session it shows the next weekday's opening (0027).
- Holidays are not known: Yahoo gives no calendar, so a holiday reads "opens tomorrow" until it has passed (0027).
- **Clocks**: market events (trades, bars, session hours) are in the exchange's clock; things the phone did ("Updated",
  "Stale since") are in the phone's clock (0031). The status line adds the phone's time in parentheses when the two
  differ, and the exchange time carries its zone name (0032).

## Personal position
- Two things per ticker (0040):
  - a **typed average**, copied from the broker. It has no amounts, so it shows in public. Each save replaces it;
    it can't be removed except with the ticker (0036, 0040). Shown with the decimals typed (0036).
  - a **trade log** (private): buys and sells, typed in or imported (below).
- **The log wins**: once a ticker has trades, both views use the average computed from them, the weighted average cost
  without fees, as the broker shows it. A sell keeps the average; at 0 shares it restarts for later buys (0041). A
  computed average is shown in the quote's decimals (0045).
- **Card** under the range bar (0040, 0042, 0047):
  - Public: empty ("Own this?..."), or the average with "Since you bought" per share. Hidden at 0 shares (0043).
  - Private: average, shares and value in one row; "Since you bought" (the whole position) and, with sells,
    "Realized (gross)" side by side below; a footer to the trades ("12 trades · last buy 20 Nov 2026 ›", or "No trades
    yet ›"). Sold out: "No shares held", the realized total and the footer (0043, 0047). Values wrap, never cut.
  - "Since avg" was renamed "Since you bought" (Italian "Da quando hai comprato") everywhere (0050).
- **Sheet** for the typed average: one field; editing shows the previous value ("Was 40.70") (0040). A dot or a comma
  as the decimal mark (0036).
- **Chart**: a dotted tertiary line at the average, with a stub and chevron at the edge when it is off the axis (0033).
  In private, the **last buy** is a tertiary diamond at its date (and time on 1D and 5D) and price, only when it falls
  in the range; the axis stretches to include it, and scrubbing snaps to it ("Your last buy · 6 Nov, 10:41") (0042,
  0045). No average line and no diamond at 0 shares (0043).

## Trades (private)
- Per ticker, opened from the private card's footer (0042). A summary line ("130 shares · avg 40.70 EUR · 13 trades";
  sold out: "No shares held · realized +631.60 EUR (gross) · 10 trades"), then the trades by year, newest first, with a
  date tile, Buy or Sell, shares, time when known, price and amount (0042, 0047).
- Sell rows show their realized gain as a badge ("+153.56 (+21.68%)") in place of the amount (0047).
- Tap edits, swipe deletes with Undo, + adds. The sheet: Buy/Sell, date, optional time in the exchange's clock, shares
  (decimals allowed), price without fees, the amount as you type and, for a sell, its realized gain (0042, 0047).
- **Shares never go below 0**: every change is replayed oldest first as you type and again when saving; a change or
  delete that would break it is refused. Trades without a time count at the start of their day (buys) or the end
  (sells) (0041, 0045). A trade can't be dated after today, typed or imported.
- **CSV import** (0041, 0045): the app's own broker-neutral format (`ticker,side,date,time,quantity,price,id`, UTF-8,
  header required); each broker's export is converted outside the app. Imported per ticker from the trades screen:
  other tickers' rows are skipped and counted, ids already saved are skipped, and a dialog shows what will be added
  and the shares and average after it. A bad row anywhere (with its line number), shares below 0 or missing columns
  save nothing. Files over 2 MB are refused unread.

## Realized gains
- Per sell: (sell price − the average at that moment) × shares sold, recomputed whenever a trade changes (0043).
  Gross: no fees, no taxes, labelled "(gross)" where it could read as net (0043, 0047).
- They survive a sell-out: a sold-out ticker stays in the watchlist (0043).
- Shown on sell rows, on the private card, in the sold-out summary, and as one quiet line at the bottom of the
  Portfolio tab (0047, 0050).
- Change amounts of 10,000 or more are shown without decimals ("+61,310 (+306.55%)"), so a badge stays on one line;
  percentages keep 2 decimals (0048).

## Portfolio tab
- **What counts** (0049): EUR tickers with trades and a saved quote. Tickers with only a typed average, and tickers in
  another currency, are listed under "Not counted" (tap opens their details). Sold-out tickers count only in realized
  gains.
- **Total card** (0051, 0053): private shows the total value; public "Your portfolio" and the number of holdings. Then
  two full-width lines, each a badge then its label: today's change (Σ shares × (price − previous close)) and "Since
  you bought" (value − invested), in € and % in private, % only in public. A tertiary line when prices are stale ("2
  prices stale since 10:41"). Private ends with "Invested" (the cost of the shares held, at their averages).
- **Growth by year** (0050, 0052): one row per calendar year since the first buy, newest first; the current year "So
  far", the first one "From 12 Mar". Money-weighted (modified Dietz): buys and sells in the year are weighted by the
  days they were invested, so new money isn't growth. The yearly amounts add up to the gain since you bought plus the
  realized gains. Private shows € and % and opens the year detail; public shows % only.
- **Year detail** (private): the year's return, the value at the start (0 in the first year), the money added (buys
  minus sells) and the value at the end ("Today" in the current year); then every ticker held that year with its
  year-end price and its market return over the calendar year, dividends included, highest first (0050, 0051, 0052).
  The current year's market return misses dividends paid after the year ends were fetched (accepted, 0052).
- **Composition** (private, 0051): "N holdings, by value", each with its weight in % (one decimal), value and a bar;
  tap opens the details. Public shows no composition and no not-counted list.
- **Year-end prices** (0052): one daily-bar request per ticker, from 1 Dec of the year before its first trade, refused
  unless Yahoo answers daily bars; each year's last bar is saved (`close` for values, `adjclose` for market returns).
  Fetched only when the tab is first shown, then again only when a new year has closed or an older trade needs earlier
  years. 5 s between requests, also when the loading restarts; a 429 stops all fetching until the app restarts; other
  failures are tried again when the app is opened. One progress bar while loading; a year missing a price is left out
  with a note saying whether it will be retried or Yahoo has no price.
- Pull-to-refresh runs the watchlist's quote refresh; year ends are not fetched again (0052).
- Known limit: Yahoo's closes are split-adjusted while trades are in real shares, so a split after a trade misstates
  the years before it (0052).

## Auto-refresh (details screen)
- Off each time the screen opens. The sync button starts it while the market is open (0035).
- Every 15 s from the end of the previous fetch, one 1D chart request per poll. That updates the header, the saved
  quote, the market info and, on 1D, the chart (0037, 0038).
- Stops when tapped again, when the screen or the app stops, when the market closes, or at the first failure or 429,
  with no retry. A failure marks the header stale (0035, 0037).
- While it runs, the last point of the 1D chart has a pulsing halo. A poll that lands during a scrub is held until
  the finger lifts (0037).
- Yahoo's `.MI` prices are about 15 minutes late, so this shortens the wait for the next delayed trade; it is not a
  live feed (0038).

## Data and storage
- **Yahoo** via Cronet (Chrome's network stack), because Yahoo refuses non-browser TLS fingerprints. Cookie, then
  crumb, then the chart endpoint. A rejected session is renewed and retried once; a 429 is not (0001, 0013).
- **One request at a time**: `SourceGate`, a cancellable lock shared by every repository (0016, 0030).
- **Room** (schema v7): the watchlist with each ticker's last quote and list position, the typed average, the trades,
  and each ticker's year-end prices with the years they cover. Chart points are not stored (0015, 0016, 0031, 0036,
  0040, 0042, 0052). Removing a ticker deletes its average and trades with it (Undo puts them back); its year-end prices
  are market data and stay.
- **Session cache** in memory: chart responses per ticker and range, 2 minutes for 1D and 5D, 1 hour for the rest.
  Retry and polling bypass it. Switching ranges waits 250 ms, so tapping through the bar sends one request (0026, 0027).

## App shell
- English and Italian. On Android 13+ the language can also be set per app (0017).
- Theme from the Doop tokens: light, dark and Material You dynamic colour, with gain and loss colours kept fixed under
  dynamic colour (0011).
- Icon: a smiling euro banknote over a rising chart, adaptive and with a themed (monochrome) layer (0008, 0010).
  Splash from `core-splashscreen`, the same style on every API level (0012).

## Code layout
- `data/`: models, `QuoteSource` and its Yahoo implementation, the repositories, `db/`. Nothing outside it knows about
  Yahoo, Cronet or Room. `ui/common/` holds what two or more screens share, `ui/<screen>/` one screen each,
  `ui/theme/` the theme. A screen never imports another screen's package (0030).

## Build and release
- Built and checked locally on an emulator; no CI (0002).
- Release APKs are signed with a local keystore; distribution is manual through GitHub Releases (0020).
  `scripts/release.sh` does the whole release (0021).

## Not done yet
Open points and ideas live in [roadmap.md](roadmap.md). Known limits: no holiday calendar, the price delay is not
stated in the app, a typed average can't be removed on its own, no currency conversion in the portfolio, splits are
not handled, and the public/private switch has no fingerprint or PIN.
