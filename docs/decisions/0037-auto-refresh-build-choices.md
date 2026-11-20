# 0037: Auto-refresh, choices made in the build

Status: accepted (2026-11-20), adds to 0035

## Context
Decision 0035 left open what one poll fetches, whether it updates the saved quote, and where the sheet's exchange name
and delay come from. Some behaviour showed up only while building.

## Decision
- **One poll is one 1D chart request**, bypassing the session cache. Its `meta` quote is saved as the ticker's quote
  (`HistoryRepository` already does this for a fresh 1D response, decision 0029), so the header and the watchlist row
  follow it; the market info is refreshed; the chart is replaced only while 1D is shown. On another range the poll still
  runs and refreshes the 1D cache for when the owner switches back.
- **The first poll is 30 s after the tap**, not at once: the screen's own load is the "previous fetch", and toggling the
  icon on and off cannot send requests.
- **The halo shows only on 1D**: a poll does not touch the last point of the longer ranges, so a halo there would say
  something false. The tonal icon says polling runs on every range.
- **Scrub**: a poll that lands during a drag is held and shown when the drag ends, so the point under the finger does
  not jump.
- **Failure**: the header goes stale through the watchlist's failed mark (`WatchlistRepository.markFailed`), as after a
  failed refresh; the next successful fetch clears it. The chart keeps what it shows.
- **Stops** also when the marker and icon are hidden (the chart failed or the hours became unknown), besides the rules of
  0035. The loop is `pollWhileOpen` in `ui/details/AutoRefresh.kt`, with a unit test for its stop rules;
  `DetailsAutoRefreshTest` covers what the view model does with each poll (held during a scrub, a failure stops it and
  marks the header stale, no retry). The view model takes a clock, so the test does not depend on the day it runs.
- **One sync button**: an `IconButton` whose colours switch to filled tonal when on, not two components swapped, so
  TalkBack keeps its focus on the button when it is toggled.
- **Sheet**: the title is Yahoo's `fullExchangeName` ("Milan" for `.MI`, not "Borsa Italiana"), then the quote's short
  exchange name, then the ticker code. Yahoo's chart `meta` gave no delay on 2026-11-20 (`exchangeDataDelayedBy` absent
  or 0), so the delay row is hidden; it appears if a response carries one. "Today's session" became "Trading hours"
  (the session shown is the next one when the market is closed); the hours carry the exchange's zone name when the
  phone's clock differs. "Last quote" uses the header's stamp format.

## Consequences
- The ~15 minute delay is not stated anywhere in the app: the sheet's "Last quote" and "checked" times show it
  indirectly. Stating it needs a source for the delay per exchange (roadmap).
