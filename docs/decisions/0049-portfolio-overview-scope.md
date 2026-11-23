# 0049: Portfolio overview (M4 scope) and the milestones after it

Status: accepted (2026-11-23), scopes M4 of 0040

## Context
Decision 0040 put "totals and allocation across tickers" in M4. Everything up to M3 looks at one ticker at a time:
the watchlist row, the details screen, the per-ticker trades screen. The owner wants the big picture of what they
own: the grand total and how it is split. Further wishes came up in the same brainstorm: grouping by asset type
(ETF, single stock, Italian government bonds), an ETF's theme and geography (world index vs. semiconductors, too
much US exposure), a compound growth projection and a withdrawal simulation. Those are too much for one milestone.

## Decision
- **Milestones after M3**, each on its own branch from main as in 0040:
  - **M4, portfolio overview**: the second tab, the grand total, composition by ticker (this decision).
  - **M5, asset types**: a type per ticker and the breakdown by type (ETF, single stock).
  - **M6, projection**: an expected yearly return per ticker and the compound growth of the portfolio. It gets its
    own brainstorm (contributions, inflation, horizon).
  - **M7, withdrawal simulation**: selling a share of the portfolio each year, net cash out vs. remaining growth.
  - Later: theme and geography exposure. It needs each ETF's holdings; no free source is known yet.
- **Navigation**: a bottom navigation bar with two destinations, Watchlist and Portfolio. This is the second real
  destination that 0042 waited for.
- **Portfolio in both views**
  - Private: the € figures below.
  - Public: composition as percentages of the whole only, with no € amounts (relative figures only, like the
    average price in 0040). Whether a portfolio-wide % change is shown in public is a design question for the frames.
- **Grand total (private)**: current value (shares × latest price, summed), invested (cost basis from the trade
  log), unrealized gain in € and %, today's change in €, and realized gains across all tickers, total and per year
  (the totals 0043 left to M4). Gross, as in 0043.
- **Composition**: one row per ticker with shares held, its value and its weight in % of the total, sorted by
  weight, with a bar or a donut (chosen in the frames).
- **What counts**
  - Only tickers with trades and shares held. A ticker with only a typed average has no shares, so it is listed
    below the total as not counted (no trades) rather than hidden.
  - Sold-out tickers are not in the composition; their realized gains count in the realized totals.
  - EUR only. A ticker in another currency is left out with a note; currency conversion goes to the roadmap.
- **Staleness**: the total is marked when any price in it is stale, with how many and since when, following
  "correct over available".

## Consequences
- BTPs are out of scope for now, accepted by the owner. They are not in the app today and do not fit M4's per-share
  maths: they are quoted per 100 of face value, the quantity is a face value, they pay coupons and accrue interest,
  and Yahoo's coverage of them is unknown. They stay an open item to investigate (data source first, one request at
  a time, CLAUDE.md), possibly as an intermediate milestone between M4 and M5. Until then the total covers only what
  is in the app.
- M4 is designed in Doop on a page "M4 · Portfolio" before it is built; the frames settle the layout, the chart type
  and the public view.
- Value over time (a history chart of the portfolio) is not in M4: it would need historical prices for every ticker.
