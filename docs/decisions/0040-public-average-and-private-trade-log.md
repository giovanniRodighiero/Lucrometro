# 0040: Public average, private trade log

Status: accepted (2026-11-22), supersedes parts of 0033 and 0034

## Context
The last-buy diamond (0034) marks a date but is drawn at a price: the line's value that day. On 1D and 5D that point
has no meaning, and on 1D the whole chart is the buy's day. The app knows when the owner bought but not at what price,
so no position on the chart is right.

The owner also wants to keep every purchase, as the first step towards tracking a portfolio, and to check the market in
public without showing how much is invested. Each save today (0033) is a summary (average, shares, date of the last
buy), not a trade, so it can't become a buy history.

## Decision
- **Two separate things per ticker.**
  - **Average price** (public): typed from the broker, as in 0033. It's relative (no amounts), so it can be shown in
    public with the line on the chart and the change since the average.
  - **Trade log** (private): one entry per trade (date, shares, price, fees), typed in or imported from the broker's
    CSV (Directa). Starts empty for every ticker; the saved summaries are not converted.
- **The log wins.** Once a ticker has trades, both views use the average computed from the log, using the weighted
  average cost with fees included (as Italian brokers do). The typed average is kept only as the fallback for tickers
  without trades, so the two views never show two different averages.
- **Public and private views.** One tap switches between them (no authentication yet; fingerprint or PIN is in the
  roadmap). Public: quotes, chart, average. Private adds shares, value, € gain or loss, the trade history, and the
  latest buy on the chart at its date and price. Designed in a later session (M2).
- **Milestones**, each on its own branch from main, merged before the next starts:
  - **M1, average only**: drop the last-buy date, the diamond and the shares from the card, the sheets and the chart.
    A Room migration keeps each ticker's latest average. The value of the position is gone until M2; accepted.
  - **M2, trade log and private view**: buys only, the form, then the Directa import.
  - **M3, sells**: realized gains; the average follows the weighted average cost.
  - **M4, portfolio overview**: totals and allocation across tickers.
- **M1 design** (Doop, 2026-11-22): `PositionCard` avg state has the average and "Since avg" only, with no hint
  and no footer. The add and edit sheets have the average field only ("Was 40.70" on edit). `PositionCard`'s full state
  and the frame "Parked for M2 (private view) · position (avg + shares)" are kept for reference only.

## Consequences
- 0033's shares, last-buy date and dated entries, and 0034's diamond, are removed by M1. 0033's "the app never
  computes the average" no longer holds once a ticker has trades (M2).
- Room: M1 needs a schema bump and a migration that keeps one average per ticker. M2 adds the trade table.
- Open for M2: Directa's CSV columns (time of the trade, fees, Tobin tax); how the buy point is placed on 1D/5D if the
  CSV has no time; the look of the switch and of the private card.
