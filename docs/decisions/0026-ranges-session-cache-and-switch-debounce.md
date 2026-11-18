# 0026: Chart ranges, session cache and switch debounce

Status: accepted (2026-11-18)

## Context
Iteration 4 of the details screen adds the range bar (decision 0023) on top of the 1M chart (decision 0025).

## Decision
- **Ranges and bars**: 5D, 1M, 3M, 6M and 1Y use daily bars; 5Y weekly; Max monthly (decision 0022). Yahoo can answer a finer
  interval for a short history, so the chart draws whatever comes back. 5D with daily bars is only 5 points: an intraday
  interval would look better but is not verified for `.MI` and has no `adjclose` (to check, see the roadmap).
- **Bar**: 7 cells for now (the 1D tab joins with the intraday chart, which also becomes the default range); the open
  range is the ViewModel's state, so it survives rotation but not a restart. Default stays 1M until then.
- **Session cache** (PRD 5), in the repository: candles per ticker and range, in memory. Entries expire after 1 hour
  (PRD said "the whole session"): the last bar is the live price, so a long-lived process would show a chart that disagrees
  with a refreshed header. Retry bypasses it, removing a ticker drops its entries (and a request still running for it is
  not cached), only results with 2+ bars are kept. A cached range shows at once with no request.
- **Switch debounce**: an uncached range waits 250 ms before it is requested, so tapping through the bar sends one request
  for the range you stop on, not one per tap (the Yahoo throttle worry, CLAUDE.md). A request that was already sent is
  not cancelled by a later selection: it finishes and fills the cache, and the screen only shows it if its range is still open.
- **Date labels**: day and month up to 6M, month and year from 1Y ("Nov 2025": a 2-digit year reads as a day).
- The screen scrolls, because the header, chart and bar do not fit at a large font scale.

## Consequences
- Each ticker costs at most one request per range per session.
- The 1D intraday range will need its own, shorter TTL (PRD 5 proposes 2 minutes).
