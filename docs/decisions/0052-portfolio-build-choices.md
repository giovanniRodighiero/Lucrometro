# 0052: Portfolio build choices (M4 build)

Status: accepted (2026-11-24), supersedes the "last bar" fetch of 0051; settles the points 0049-0051 left to the build

## Context
0049-0051 scoped and designed the Portfolio tab. Building it raised points the frames don't answer: how to get the
year-end prices, how money flows inside a year are weighed, what happens while prices load or fail, and a few
navigation details. The owner chose three of them on 2026-11-24 (progress, leave the year out, schema bump) plus the
dividend and refresh points below.

## Decision
- **Year-end prices**: one Yahoo chart request per ticker with `period1`/`period2` and `interval=1d`, from 1 Dec of
  the year before the first trade (the base of the first year's return, past any holiday) to tomorrow. The response
  must say `dataGranularity` `1d`, else it fails: Yahoo widens the bar size on long ranges, and a monthly bar would
  not end on the last trading day. The year end is the last daily bar of each closed calendar year in the exchange's
  zone. `close` gives the portfolio values, `adjclose` the per-ticker market return (one fetch, so both share a basis).
- **Storage**: Room v7 (auto-migration 6 to 7), tables `year_end` (code, year, date, close, adjclose) and
  `year_end_fetch` (code, from year, through year). One fetch replaces a ticker's rows atomically. Rows stay when a
  ticker is removed (like its trades).
- **When to fetch**: only when the Portfolio tab is first shown in a run of the app (an app used only for the watchlist
  makes no history requests), for each counted ticker that has no saved year ends, needs an earlier year (an older
  trade was added), or has not seen the last closed year. One at a time, 5 s apart, through the shared source gate.
  Every request waits 5 s after the previous one, also when the loop restarts mid-fetch (new trades, app brought back).
  A 429 stops all fetching for the rest of the process (CLAUDE.md). Any other failure marks the ticker failed until the
  app is opened or brought back, when it is tried again.
- **Loading and failure**: one linear progress indicator in "Growth by year" while any shown year still waits for a
  ticker. A year whose prices are missing for a ticker that failed is left out, with the tertiary note "Left out:
  2026, 2025. The year-end prices of SMH.MI couldn't be fetched; they are tried again when you next open the app."
  A year whose saved prices have no bar for a held ticker (a trade dated before Yahoo's history starts) is left out
  with "Yahoo has no year-end price of X for these years." and is not fetched again.
- **Yearly return**: modified Dietz. The period runs 1 Jan to 31 Dec; the first year starts on the first buy (start
  value 0) and the current year ends today (end value = today's total). Each buy or sell is weighted by the days it
  was in the period, its own day included. Gain = end - start - added; % = gain / (start + weighted flows); no % when
  that base is 0. Over all years the gains add up to (value - invested) + realized.
- **Current-year market return** per ticker: today's price over the last year end's `adjclose`. Dividends paid this
  year after the fetch are not in it (the year ends are fetched once). Accepted by the owner; fixed when the next year
  closes and the ticker is fetched again.
- **Today's change** on the total card: the sum over held tickers of shares x (price - previous close); % over the sum
  of shares x previous close.
- **Not counted** also lists tickers with trades in another currency, under "Not in EUR: the total doesn't convert
  currencies yet." (0049 counts EUR only).
- **Pull-to-refresh** on the Portfolio tab runs the watchlist's quote refresh; year ends are not fetched again.
- **Navigation**: the selected tab survives rotation and process death; back on the Portfolio tab goes to the
  Watchlist before leaving the app. A year detail restored after process death starts the year-end loading itself.
- **Year detail**: the start label is the period's start date ("12 Mar" in the first year, "1 Jan" otherwise), the end
  label "Today" in the current year; holdings sorted by market return, highest first, unknown last (0051 says sorted;
  the frame's sample rows are not).

## Consequences
- History requests: one per counted ticker the first time, then one per ticker each January. 15 tickers take ~75 s
  the first time, shown by the progress bar.
- A ticker listed during a year has no market return for that year (row shown without a badge).
- Yahoo's `close` is already split-adjusted while trades are in real share counts, so a split after a trade misstates
  every year before it, like the rest of the app (the live price against the trade log). Not handled yet.
