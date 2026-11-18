# 0033: Personal position: average price from the broker

Status: accepted (2026-11-18)

## Context
The roadmap's "Later" list had a personal position per ticker. The owner accumulates over time and wants to see,
next to the market price, the average price they paid, without turning the app into a broker. Designed in Doop on
2026-11-18 (page "Exploration · Position", then promoted to "Screens").

## Decision
- **What is stored**: per ticker, the average price the broker shows (in the ticker's currency), the number of shares
  (optional) and the date of the last buy (today by default, can be in the past: buys are often logged later). The app
  never computes the average: after a buy the owner copies the new values from the broker, so the app always agrees
  with it, fees included.
- **Each save is a dated entry**, not an overwrite; the screen shows the latest. This keeps a history from the first
  day and prepares a later full log of buys: with shares, a buy can be derived from two entries
  (Δshares, and (new avg × new shares − old avg × old shares) / Δshares).
- **No remove** for now: it waits for the full log, which will need its own editing model.
- **Chart**: a 1.5dp dotted line in the tertiary colour at the average, "Your avg" above its left end and the price in
  the gutter. It is dotted so it never reads as the dashed previous-close line of 1D. When the average is outside the
  axis range, the axis is **not** stretched to include it (that flattened the day's line into a sliver): a short dotted
  stub at the plot edge and a chevron plus the price in the gutter.
- **Card under the range bar** (Doop `PositionCard`): empty ("Own this? Add the average price..." and an "Add average
  price" text button), average only (average, change since average, hint to add shares), or full (average, shares,
  value, change since average). Avg and full end with a footer row, "Last buy 28 Oct 2026", under a divider; chosen over
  a line under the title (cramped), under the average, or as a grid cell, and it can later open the buy history.
- **Sheet** (Doop "add/edit position sheet"): average price, shares (optional), date of the buy with a date picker;
  the edit sheet shows the previous values ("Was 40.70"). Cancel and Save only.
- **Numbers** follow the app language (comma in Italian, dot in English); input accepts either as the decimal mark.
- **Chart height** 324 → 292dp (plot y 12-256, time labels at 280) for every range, so the card fits under the range
  bar on a ~900dp screen. Smaller phones scroll a little; the price, chart and range bar stay above the fold.
- **Watchlist**: unchanged. A quiet "Avg 29.50 · +39.90%" line on held rows is designed but undecided.

## Consequences
- Room schema v4 with a migration: a new table of dated position entries keyed by ticker code. Removing a ticker
  removes its entries; undo restores them.
- `PriceChart` needs the average line and the edge marker; `ChartHeight` and the plot geometry change for every range.
- The "change since average" for distributing tickers is exact today (last price vs average), but on 1M and longer the
  past part of the line uses adjusted closes (decision 0022), so where the line crosses the average in the past is
  slightly off for dividend payers. Accepted for now.
- Not designed: the date picker (stock Material), the numeric keyboard, validation messages.
