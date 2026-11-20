# Lucrometro: how the app works

Up to date with: **0.3** (decisions 0001-0038). This is the current state; read it before the decisions.
The decision numbers in brackets point to the reasoning and the options that were turned down. The starting point
was [prd.md](prd.md) (v1); where they disagree, this file wins.

## Principles
- **Correct over available.** When data can't be fetched, say so and show the last saved value as stale.
  Never show wrong or substitute data.
- **All local.** No accounts, no own server, no analytics. Everything stays on the phone.
- **Free data.** Yahoo Finance's unofficial endpoints, behind `QuoteSource` so the provider can be swapped (0001).
- **Native.** Material 3 components and system behaviour; custom drawing only for the chart (0003).
- **Gentle on Yahoo.** One request at a time across the whole app, and a 429 always stops, never retries.

## Watchlist (home screen)
- One row per ticker in the order it was added: code, name, price in the ticker's own currency, day change as a
  tonal pill (gain, loss or flat; flat when the rounded value is 0.00%). Plain rows, small FAB, no summary card (0007).
- Day change is measured from the previous session's close (0014).
- An "Updated" stamp shows the newest fetch time, in the phone's clock (0016, 0031).
- **Refresh**: when the app is opened or brought back (skipped within 60 s of the last refresh), and by
  pull-to-refresh, which always fetches. Coming back from a ticker's details is not a refresh. Tickers are fetched one
  at a time. The first 429 stops the refresh and marks the remaining tickers stale. (0014, 0016, audit after 0.3)
- **Stale**: a quote is current only if it was fetched in this app session. Otherwise, or when its fetch failed, the
  row keeps it and shows "Stale since <time>" (0016).
- **Add**: a bottom sheet where the code is pasted (no search). Input is trimmed and upper-cased, the format checked,
  duplicates rejected, then a live quote is fetched: the ticker is saved only if it resolves. Errors tell a wrong code
  from a network problem. Closing the sheet cancels the check (0013).
- **Remove**: swipe from the end, or the "Remove" TalkBack action. It is deleted at once together with its saved
  quote and position. A snackbar offers Undo, which puts it back in its old place. Only the latest removal can be
  undone (0018, 0019).

## Details screen
Opened by tapping a row. No navigation library: the open code is the whole navigation state (0024). Each opening
starts on 1D and reloads (0029). Layout from top to bottom (0023, 0035):

- **Top bar**: back, ticker code, the market marker ("● Open" / "○ Closed", never red) and the sync button. Both are
  hidden when the hours are unknown or the chart failed.
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
- Per ticker: the average price **copied from the broker**, optional shares, and the date of the last buy (today by
  default, never in the future). The app never computes the average, so it always agrees with the broker (0033).
- Each save is a new dated entry and the latest one is shown, which keeps a history for a later buy log. There is no
  remove yet, except removing the ticker (0033, 0036).
- **Card**: empty ("Own this?..."), average only (change since the average, per share), or full (shares, value, change
  for the whole position). A "Last buy <date>" footer closes it (0033, 0036).
- **Chart**: a dotted line in the tertiary colour at the average. When the average is outside the axis, a stub and
  chevron at the edge instead of stretching the axis. The last buy is a diamond on the line when its date is in the
  range (0033, 0034).
- **Input**: a dot or a comma as the decimal mark, nothing else; the average is shown with the decimals typed (0036).

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
- **One request at a time**: `SourceGate`, a cancellable lock shared by both repositories (0016, 0030).
- **Room** (schema v4): the watchlist with each ticker's last quote and list position, and the dated position
  entries. Chart points are not stored (0015, 0016, 0031, 0036).
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
stated in the app, and a position can't be removed on its own.
