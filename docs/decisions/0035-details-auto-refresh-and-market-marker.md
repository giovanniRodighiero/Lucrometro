# 0035: Auto-refresh on the details screen, and a compact market marker

Status: accepted (2026-11-20), refines 0023 and 0032 for where the market status is shown

## Context
The details screen shows the quote it was opened with. To follow a ticker the owner has to go back and refresh by hand.
Wanted: a way to start a polling that updates the quote and stop it when done, with the UI first. Yahoo's `.MI` quotes
are about 15 minutes delayed (research/data-sources.md), so this is "check again", not a live feed.

Tried in Doop, page "Exploration · Auto-refresh" (frames deleted, the final ones stay there):
- an "Auto-refresh" chip under the change pill: clear, but 38dp of height on a screen already tight with the position
  card (decision 0033);
- the sync icon beside the full market status in the top bar: the long labels of 0032 ("Closed · opens 09:30 EST
  (15:30)") collide with it, and an icon alone says little;
- the market status as a strip under the header: works, 24dp, but the status is still wide text and the screen gains
  a line;
- a compact marker with the details behind a tap (chosen).

## Decision
- **Top bar**: back arrow, ticker, a marker ("● Open" or "○ Closed": a dot and one word, 48dp touch target) and the
  sync icon button (48dp). The full status label leaves the top bar.
- **Marker**: filled gain-colour dot for open, hollow outline dot for closed, neutral text. Never red for closed:
  red already means loss in the chart and the pills. Hidden when the hours are unknown or the chart failed, as 0023
  says for the label.
- **Details sheet**: tapping the marker opens a modal bottom sheet with the exchange name, the full status ("Open ·
  closes 17:30", with the exchange's clock and the phone's in parentheses as in 0032), today's session hours, the
  last quote time, the delay (about 15 minutes) and the auto-refresh state ("Every 30 s · checked 14:47").
- **Icon**: plain when off, tonal (secondary container) when on, 38% and disabled when the market is closed. Its
  content description changes ("Start auto-refresh", "Stop auto-refresh", "Auto-refresh unavailable, market closed").
  Hidden together with the marker when the hours are unknown or the chart failed (nothing to poll against; the chart
  area has its own Retry).
- **Halo**: while polling, the last point of the chart has a soft pulsing halo in the line's colour (gain, loss or
  neutral). Not shown otherwise. It means "polling is running", not "market open".
- **Behaviour**: starts off every time the screen opens (not saved). Polls every 30 s, counted from the end of the
  previous fetch, one request at a time through the same `QuoteSource`. Fixed interval, no picker. Stops when the
  icon is tapped, the screen or app stops, the market closes, or a fetch fails: no retries, the header goes to the
  stale state, and a 429 stops it (CLAUDE.md rules for live requests).
- Left out for now: a countdown progress line under the top bar (constant motion; the halo already says "running"),
  and a brief tint of the price when a poll brings a new value.
- No WebSocket: Yahoo's stream is unofficial and, for delayed `.MI` data, would add little over polling. The check
  stays in the roadmap.

## Consequences
- `DetailsTopBar` is rev 5 in Doop: props `market` none|open|closed, `refresh` off|on and `title`; the `status` text
  prop is gone. Every details frame, the approved position frames included (they keep their sign-off), now shows the
  marker and the icon. The label cases board (`frames/market-status-cases`) now shows the sheet's status line.
- The sheet and the halo are new UI in `ui/details/` (decision 0030). New strings in English and Italian (0017).
- To settle in the build: what one poll fetches. The 1D chart response carries the same `meta` as the quote
  (research, 2026-11-18), so one chart request per poll could refresh both the header and the chart. The scrub
  and the range cache (0026) must not be disturbed by a poll.
- Request budget: 2 requests a minute for one open ticker while it runs.

## Frames (docs/design/doop/frames/)
- `details-auto-refresh-on`: marker, tonal icon, halo on the last point.
- `details-market-closed`: "Closed" marker, icon disabled, stamp naming the session day, no halo.
- `details-market-sheet`: the sheet over the "on" screen.
- `auto-refresh-spec`: the three signals and the rules, as text.
- "Auto-refresh off" is the existing `details-1d` (and every other details frame): same screen, new top bar.
- `market-status-cases`: the status line texts of decision 0032, now for the sheet.

## Changes from the current build
Before: the top bar's `actions` hold `MarketStatusLabel` (dot plus the full text); no polling; the chart's last point is
a plain 5dp dot. To see the old design, `git show 38e73f3:docs/design/doop/frames/details-1d.png`; the design delta is
`git diff 38e73f3 -- docs/design/doop` (the commit that adds these frames is the delta).
- `DetailsScreen.kt`: replace `MarketStatusLabel` in `actions` with a market marker (dot and one word, 48dp, opens the
  sheet) and a sync `IconButton` (toggle state, tonal when on, disabled when closed). Both shown only when the market
  info is known and the chart did not fail, the condition used today.
- `MarketStatus.kt`: the text logic (`marketStatus`, `withPhoneClock`, 0032) stays and feeds the sheet's status line;
  the `Row` with the dot moves to the marker.
- New `MarketSheet.kt` (`ModalBottomSheet`, decision 0030 puts it in `ui/details/`): status line, session hours, last
  quote, delay, auto-refresh state.
- `DetailsViewModel.kt`: an auto-refresh state, the time of the last check, and a polling job in `viewModelScope`
  (30 s after the previous fetch ends, `history.history(code, range, bypassCache = true)`, stop on failure). The screen
  stops it when it leaves the foreground (`LifecycleStartEffect`) and when the market status turns Closed.
- `PriceChart.kt`: a `live` flag; the last point gets a 12dp halo in the line's colour (alpha 0.32 to 0.08, about 1.6 s,
  `rememberInfiniteTransition`) behind the 5dp dot, only while polling and not while scrubbing.
- `strings.xml` and `values-it/strings.xml`: Open, Closed, the three icon descriptions, the sheet labels (0017).
- A unit test for the polling loop's stop rules (failure, cancellation) is worth having (CLAUDE.md: error mapping and
  cancellation).

To settle while building:
- The sheet's exchange name ("Borsa Italiana") and the delay row ("About 15 minutes") are not in the app's data
  today. Check what the chart `meta` carries (`fullExchangeName`, `exchangeDataDelayedBy`); show a row only when known.
- Whether a poll updates the saved quote shown in the header (it comes from the watchlist entry), or only the chart.
