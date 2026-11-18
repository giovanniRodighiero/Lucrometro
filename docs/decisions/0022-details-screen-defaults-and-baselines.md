# 0022: Details screen defaults, baselines and stale header

Status: accepted (2026-11-17)

## Context
The details screen (PRD section 4) left several choices open: which range opens first, what the
scrub change is measured against, how the header behaves when the chart fails, which price series
the ranges use, and how long chart points are kept.

## Decision
- Default range is 1D (PRD said 1M). 1D is the last trading session when the market is closed.
- The 1D change (header and scrub) is measured against the previous day's close, so the scrub
  change agrees with the header's day change.
- Range changes (5D and up, and the range summary) use adjusted closes, so distributions do not
  show up as price drops. Raw vs. adjusted is unverified against the Yahoo data we use; see
  the open items in `docs/research/data-sources.md` and roadmap item 1.
- If the chart fetch fails but a saved quote exists, the header shows that quote marked
  "stale since HH:mm". The chart area still shows "Couldn't fetch" with retry (no stale chart).
- 5Y and Max use coarser points (weekly or monthly). Only the points needed for the view are
  kept; intraday and daily points are not kept beyond the session cache.
- No remove action on the details screen in v1; remove stays on the watchlist row.
- The details screen is designed in Doop before it is built, when the owner says so.

## Consequences
- The PRD is updated for the default range, the baseline and the stale header.
- Adjusted closes are assumed, not yet verified. Until roadmap item 1 confirms that the Yahoo
  chart endpoint returns `adjclose` for these tickers, the UI must not claim "adjusted" to the user.
- The 1D baseline needs the previous session close, so the 1D request must return it (or a
  separate daily request does).
