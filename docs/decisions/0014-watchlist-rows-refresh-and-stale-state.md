# 0014: Watchlist rows, refresh and stale state

Status: accepted (2026-11-17)

## Context
Decision 0013 left the watchlist rows as placeholders. The PRD asks for one row per ticker with price,
day change, an "updated HH:mm" stamp, auto-refresh on open, pull-to-refresh, and a stale mark when a
fetch fails. The Doop snapshot (`QuoteRow`, `ChangeBadge`, `TopAppBar`, frame `watchlist-ticker-added`)
had everything needed; Doop itself was not called.

## Decision
- **Quote data**: `Quote` gains `name` (Yahoo `longName`, else `shortName`), `previousClose`
  (`chartPreviousClose`, which with `range=1d` is the close before the last session) and `priceDecimals`
  (`priceHint`). Day change % is computed from price and previous close; it matched Yahoo's own
  `regularMarketChangePercent` on ENI.MI.
- **Repository**: holds `WatchlistEntry(code, saved: SavedQuote?, failed)`. Add saves the validation quote,
  so a new row shows its price at once. `refresh()` fetches tickers one at a time; a failure keeps the last
  quote and sets `failed` (never substitutes data). Only one refresh runs at a time. `refreshedAt` is set only
  when at least one ticker was updated, so the stamp never claims a refresh that fetched nothing.
- **Refresh triggers**: pull-to-refresh (`PullToRefreshBox`) always fetches; `LifecycleStartEffect` refreshes
  when the screen starts, skipped if the last automatic refresh was under a minute ago (rotation, quick app switch).
- **Row**: a plain `Row` laid out as the Doop `QuoteRow` (min 72dp, 8/16dp padding), not `ListItem`:
  `ListItem` kept its three-line height after the stale note disappeared (seen on the emulator).
- **Formatting**: numbers in the device locale, the source's decimals (2 if unknown), currency code after the
  price. Times follow the system 12/24h setting; the date is added when not today. The change pill's
  direction follows the rounded value, so 0.00% is flat.

## Consequences
- Still in memory: the list and last quotes are lost when the process dies until Room lands (PRD 5).
- The "Couldn't fetch" row (failed, no saved quote) can't happen yet, since add always saves a quote.
  It will once Room restores codes without quotes.
- Rows are not clickable yet: the details screen does not exist.
- `ChangeBadge` lives in `ui/watchlist/`; move it to a shared package when the details screen uses it.
