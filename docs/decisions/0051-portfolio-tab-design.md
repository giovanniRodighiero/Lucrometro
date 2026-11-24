# 0051: Portfolio tab design (M4 design)

Status: accepted (2026-11-24), settles the open points of 0049 and 0050

## Context
Decisions 0049 and 0050 scoped M4 and left the look to the Doop frames. They were drawn on 2026-11-23 and 2026-11-24
on the page "M4 · Portfolio" and the owner signed off all nine (snapshot in `docs/design/doop/`, branch
`portfolio-overview`). This decision records what the frames settle, and what the build has to work out, so the
build can be done from the repo alone.

## Decision
- **Navigation**: a Material 3 navigation bar (`NavBar`) with Watchlist and Portfolio, in public and private alike.
  The watchlist keeps its title ("Lucrometro") and the add button moves up above the bar. Details, trades and year
  detail are pushed screens without the bar. Both tabs have the wallet in the top bar; the Portfolio top bar has the
  title "Portfolio" and the same "Updated HH:mm" stamp as the watchlist.
- **Private Portfolio tab**, one scrolling list, top to bottom:
  1. **Total card** (`PortfolioSummary`): "Total value" (display size), today's change in € and % with "Today",
     then "Invested" and "Since you bought" side by side. When any price in it is stale, a tertiary line under
     today's change: "2 prices stale since 10:41" (count and the oldest time).
  2. **Growth by year** (`YearGrowthRow`), with the subtitle "New buys don't count as growth.": one row per
     calendar year since the first buy, newest first; the current year has the note "So far", the first year
     "From 12 Mar" when the first buy was after 1 Jan. Amount and % in a badge, and a chevron to the year detail.
  3. **Composition** (`HoldingRow`), headed "N holdings, by value": ticker, name, weight in % (one decimal),
     value in EUR, and a 4dp primary bar for the weight out of 100%. Sorted by weight, largest first. One colour
     for every row, no donut: a colour per ticker would need fixed hues and break dynamic colour. Tap opens the
     ticker's details.
  4. **Not counted**: tickers with only a typed average, with "Average price only: add their trades to count
     them." Tap opens the ticker's details.
  5. **Realized**: one quiet line at the very end, text only (no badge, no colour): "Realized from sells, all years
     (gross)" and the signed amount in EUR.
- **Year detail** (private only, a pushed screen titled with the year): a card with the portfolio's return for the
  year (badge, amount and %), then "1 Jan", "Added" and "31 Dec" values in EUR (for the current year the end is
  today's value; for the first year the start is 0), and the note "EUR. Added = buys minus sells. The % weighs each
  buy by how long it was invested." Below, "Each holding, full year": every ticker held at any point that year,
  sold-out ones included, as a watchlist-style row with the year-end price and the calendar-year market return from
  `adjclose`, sorted by return, highest first.
- **Public Portfolio tab**: the total card with "Your portfolio", the number of holdings, today's change and the
  change since you bought in % only; then growth by year in % only, rows not tappable; then "Switch to private to
  see what you hold." No composition, no not-counted list, no realized line.
- **Empty** (no ticker with trades): a card "Nothing to add up yet" with "The portfolio counts the tickers you have
  trades for. Open a ticker, then its trades, to add them or import them from your broker.", then the not-counted
  list if there is one.
- **Numbers**: amounts follow 0048 (from 10,000 up without decimals in change badges); the total value and invested
  keep their decimals. The yearly amounts add up to the gain since you bought plus the realized gains: the build
  should check this identity in a unit test. Weights are rounded independently and may not sum to 100.0%.
- **Rename**: "Since avg" becomes "Since you bought" on the ticker position card too (string
  `position_since_average`: EN "Since you bought", IT "Da quando hai comprato"), in the M4 build.

## Consequences
- **Data**: year-end closes per ticker. Yahoo's long ranges give a coarser history (`range=max&interval=1mo` came
  back weekly for a ticker listed in 2020, see `docs/research/data-sources.md`): take the last bar of each calendar
  year, `close` for the portfolio's values and `adjclose` for the market return. Past years never change: save them
  (a Room table, schema bump and migration) and fetch each ticker once, then only when a new year closes. The
  current year uses the latest quote. Requests one at a time, spaced, stop at the first 429 (CLAUDE.md).
- **Return per year**: modified Dietz. Gain = end value − start value − net added; base = start value + each buy or
  sell amount (sells negative) weighted by the share of the year left after it. Values use shares held at that date
  (replayed from the trade log) × the close. Not counted, non-EUR and sold-out-before-the-year tickers are excluded.
- **Not drawn, decide in the build**: loading the yearly history the first time (per-row placeholder vs a single
  progress indicator), and a ticker whose history failed (leave its year out with a note, or show the year as stale).
- **To check in the build**: TalkBack on the new rows and the nav bar, Italian labels in the total card's two columns,
  dark theme. The owner accepted that badges wrap to two lines at large font sizes.
- Doop: `PositionCard` keeps "Since avg" until M4 merges; then rename it and move the M4 frames into "Screens".
