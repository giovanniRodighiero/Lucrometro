# 0043: Realized gains and sold-out tickers (M3 scope)

Status: accepted (2026-11-22), scopes M3 of 0040

## Context
Decision 0040 put "sells and realized gains" in M3. M2 (0041, 0042) already records sells: the trade sheet has Buy
and Sell, the import reads both, a sell keeps the average and the average starts again at 0 shares. What M2 does not
show is what each sell earned, or what a ticker looks like once every share is sold. The M3 frames are drawn after
M2 is built, so they start from the real screens and the owner's real history.

## Decision
- **Realized gain of a sell** = (sell price − the average at that moment) × shares sold, computed by replaying the
  ticker's trades oldest first (0041's method). Editing or deleting any trade recomputes them.
- **Gross only**: no fees (0041) and no taxes. Every realized figure is labelled as gross where it could be mistaken
  for net.
- **Realized gains are history.** They survive a full sell-out; the reset at 0 shares only restarts the average for
  later buys.
- **A sold-out ticker stays in the watchlist**, with no shares held. Nothing else changes on the watchlist.
- **Position card at 0 shares**
  - Private: "No shares held", the ticker's realized total (gross), and the footer to the trades screen. No average,
    value or "Since avg".
  - Public: the card is hidden. There is no average to show, "Own this?" would be wrong, and realized amounts are
    private.
  - Chart: no average line and no last-buy diamond, since there is no open position.
- **Where realized gains show**
  - Each sell row on the trades screen shows its realized gain (signed, gain or loss colours with an arrow, as
    `ChangeBadge`) in place of the amount; the amount stays in the edit sheet.
  - The private card gets a "Realized" line whenever the ticker has at least one sell, also while shares are held.
  - Yearly totals and totals across tickers wait for M4 (portfolio overview).

## Consequences
- Later, not in M3: an optional `fees` column (0041 already allows adding it) so realized gains can match the
  broker's statement, and an estimate of the net after Italian capital gains tax (26%, 12.5% on government bonds,
  losses offset against gains). Both are in the roadmap under "Later".
- The public view shows nothing for a sold-out ticker; the quote and chart are unchanged.
- M3 is designed in Doop after M2 is built and merged, against the built screens.
