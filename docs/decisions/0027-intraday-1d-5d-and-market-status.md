# 0027: Intraday 1D and 5D charts, and the market status

Status: accepted (2026-11-18)

## Context
Iteration 5 of the details screen (decisions 0022, 0023, 0026): 1D becomes the default range, 5D gets a real line, and the top
bar shows whether the market is open.

## Decision
- **Data**: `HistoryRange.DAY_1` is `range=1d&interval=5m` (about 100 bars for a Milan session, fewer nulls than 1-minute
  bars), `DAYS_5` is `range=5d&interval=30m` (79 clean bars, see `docs/research/data-sources.md`). Both are plain closes: intraday
  bars have no `adjclose`. `history()` returns a `History`: the candles plus the previous close (`chartPreviousClose`), the
  regular sessions the range covers, the current session (`currentTradingPeriod.regular`) and the exchange timezone. A
  response with no bars at all (1D before the first trade) is an empty history, not an error.
- **1D chart**: time axis across the trading session (09:00 to 17:30), so the line stops at the last bar and the rest stays empty;
  labels every 2 hours and the close; the previous close is a dashed baseline with its price in medium weight. The 1D change
  (pill colour, TalkBack text) is measured from the previous close, and the readout shows "Previous close" with no pill
  (the header has the day change). Without a previous close it falls back to the first bar and a "From" readout.
- **5D chart**: bars evenly spaced (by index), weekday labels at the start of each day, so the overnight and weekend gaps leave no holes.
- **Empty 1D** (not in Doop): a card "No trades yet" without Retry. Other ranges with fewer than 2 bars stay "Couldn't fetch".
- **Cache**: intraday ranges expire after 2 minutes (PRD 5), the others after 1 hour (decision 0026).
- **Market status** (top bar, decision 0023): from the current session and the exchange timezone, taken from the latest chart
  response of any range. Inside the session: "Open · closes 17:30". Before it: "Closed · opens 09:00" (today) or "Closed · opens
  Mon 09:00". After it: the next weekday at the same time. Times are the exchange's, in the phone's 12/24h style. Hidden until a
  response arrives and while the chart fetch has failed. It re-reads the clock every 30 seconds.
- **Not covered: exchange holidays.** Yahoo gives no calendar, so a holiday reads as "opens tomorrow" until the day has passed.
  Decision 0023 wanted "Market closed" when the next opening is uncertain: not done, because there is no signal for it yet.

## Consequences
- The 5D change is on plain closes: an ex-dividend drop inside the 5 days would show as a loss (rare and small).
- A weekend and a holiday have not been seen live (roadmap item 1): what `currentTradingPeriod` and a 1D range return then.
- `QuoteSource.history` now returns `History` (the fakes in the tests changed).
