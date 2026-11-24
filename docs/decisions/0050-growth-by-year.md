# 0050: Growth by year leads the portfolio; realized gains step back

Status: accepted (2026-11-23), supersedes parts of 0049

## Context
0049 put realized gains per year on the Portfolio tab and left "value over time" for later, since it needs
historical prices. Reviewing the first M4 frames, the owner said realized gains come from rare, short-term sells; what
matters is how the long-term holdings grow, year by year (a growth percentage for each calendar year). The owner also chose
to hide the composition in public.

## Decision
- **Growth by year** is the Portfolio tab's main section, right under the total card, years newest first. The
  current year is marked "So far"; the first year starts at the first buy ("From 12 Mar").
  - **The portfolio's return per year** (private: € and %; public: % only): money-weighted, so buys and sells
    during the year are not counted as growth: (value at year end − value at year start − money added) / (value at
    year start + money added, each amount weighted by the share of the year it was invested). Money added = buys
    minus sells. The yearly € figures add up to the gain since you bought plus the
    realized gains.
  - **Year detail** (private only, tapping a year): the portfolio's figures (value on 1 Jan, money added, value
    on 31 Dec) and each ticker held that year with its own market return for the full calendar year, from the
    price adjusted for dividends. It can differ from the owner's return on that ticker when bought during the year.
  - Data: each ticker's year-end prices, from one price-history request per ticker; past years never change, so
    they are saved and fetched once (CLAUDE.md's rules for live requests).
- **Realized gains** stay, but out of the way: one quiet line at the very bottom of the page ("Realized from sells,
  all years (gross)"), not on the total card and not per year.
- **"Since avg" becomes "Since you bought"** (Italian "Da quando hai comprato"), everywhere: the portfolio card and
  each ticker's position card. On the whole portfolio there is no single average price, and the owner found the old
  label unclear. It reads as a time frame, like "Today" above it; the badge carries the sign.
- **Public view**: the number of holdings, today's change and the change since you bought in %, and growth by
  year in %. No composition (no tickers, no weights) and no year detail.

## Consequences
- Dividends paid out in cash are not in the trade log, so a distributing ticker's growth shows a little less than it
  earned. Accumulating ETFs are not affected.
- A chart of the portfolio's value over time stays in "Later": it needs a price for every day, not one per year.
- M6 (projection) can start from these yearly returns when proposing an expected return per ticker.
