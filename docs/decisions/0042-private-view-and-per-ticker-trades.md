# 0042: The private view and per-ticker trades (M2 design)

Status: accepted (2026-11-22), settles the open points of 0040 and 0041

## Context
Decision 0040 planned M2 (trade log and private view) and left open the look of the switch and of the private card,
and where the trades live. Decision 0041 set the trade CSV and left open how to place a trade without a time, what to
do with other tickers in the file, and where the import lives. M2 was designed in Doop on 2026-11-22 (pages
"M2 · Private switch" and "M2 · Private view"); the owner signed off every frame.

## Decision
- **Switch**: a wallet icon button in the top bar of the watchlist and of the details screen. Outlined in public;
  filled on tertiary container in private, so it never reads as the pressed auto-refresh button (secondary
  container). One tap, no authentication (0040). Chosen over an eye, a person, a lock and gestures (hidden, and they
  conflict with TalkBack).
- **No bottom navigation in M2.** Trades belong to a ticker, so there is one top-level screen only; the wallet's
  tint is the only sign of private mode. A navigation bar comes back when there is a second destination (M4
  portfolio).
- **Trades are per ticker**: from the details screen, the position card's footer opens that ticker's trades screen.
  Add, edit and delete happen there; the screen exists only in private.
- **Position card states**: public `empty`, `avg` (typed, pencil) and `avg-traded` (computed from trades, no pencil);
  private `private` (average, shares, value, total gain "Since avg", footer "12 trades · last buy 20 Nov 2026 ›"),
  `private-avg` and `private-empty` (footer "No trades yet ›"). The private empty text invites adding trades or the
  average. The "Since avg" badge is per share in public and for the whole position in private.
- **Last buy on the chart** (private only): a tertiary diamond at the trade's own date or time and price, so it can
  sit off the line, and the price axis stretches to include it (unlike the average line, which gets an edge marker).
  On 1D and 5D it is drawn only if the trade has a time; on every range only if the trade falls inside it. Scrubbing
  snaps to it, and the readout shows "Your last buy · 6 Nov, 10:41" and "10 shares at 42.85 EUR".
- **Trades screen**: top bar with "Trades", the ticker, and an import icon; a summary line ("130 shares · avg 40.70 EUR
  · 13 trades"); trades grouped by year, newest first. Each row has a date tile (day and month), no icon, so nothing
  echoes the add button; the word Buy (tertiary) or Sell (primary) in medium weight, then the shares; the time, or
  nothing when unknown; the price and the amount (no fees). No gain or loss colours: a sell is not a loss. Tap edits,
  swipe deletes with an undo snackbar, the + button adds. An empty state offers Import CSV and Add trade.
- **Add/edit trade sheet**: Buy/Sell segmented button, date (picker, starts on today), optional time in the exchange's
  clock, shares (decimals allowed), price per share without fees, and the amount as you type. Edit adds Delete.
  A change that would take the shares below 0 at any point shows the error on the field and disables Save; a delete
  that would do so is refused with a message.
- **Import is per ticker**, from the trades screen: rows for other tickers are skipped and counted, so the broker's
  full file can be used on each ticker. Before saving, a dialog shows the new trades (buys and sells), their dates,
  what was skipped (other tickers, ids already saved), and the shares and average after the import; when a typed
  average exists it says the computed one replaces it. Errors use the same dialog and save nothing: a bad row (with
  its line number), shares below 0, no rows for this ticker, missing columns. "Nothing new" is a plain notice.

## Consequences
- Replaces 0041's open point on other tickers: they are skipped, not added to the watchlist.
- New Doop components: `TradeRow`, `TradesTopBar`, `SegmentedButton`, `Dialog`; `TextField` gains an error state and
  calendar or clock icons; `PositionCard` gains the private states; `DetailsTopBar` and `TopAppBar` gain the
  `privacy` prop. The "Parked for M2" frame was deleted.
- The trades screen is a third screen, which is 0024's trigger to move to Navigation 3: the M2 build supersedes 0024
  with its own decision.
- Untested: TalkBack labels for the wallet, the date tile and the diamond; Italian strings for every new text.
