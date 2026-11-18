# 0029: Reopening details, the header following the chart, and the scrub for TalkBack

Status: accepted (2026-11-18)

## Context
Three gaps found while polishing the details screen (decisions 0022-0028).
- The details ViewModel is kept per ticker for the activity's lifetime, so reopening a ticker showed the first visit's header,
  range and chart, even after the session cache (decisions 0026, 0027) had expired.
- The header shows the saved quote, while the chart's last bar is fetched live, so they could disagree.
- The scrub is a drag, which TalkBack cannot do (decision 0028 left that open).

## Decision
- **Opening** a ticker's details starts on the default range (1D, decision 0022) and reloads: from the cache while it is
  fresh, otherwise from the source. Once per opening: the flag is saved with the composition, so a rotation neither resets
  the chosen range nor refetches.
- **The saved quote follows a 1D chart response**: it carries the current quote (price, time, previous close), so the
  repository saves it (and clears the ticker's failed mark) right after the fetch, at no extra request. Only a 1D response
  that was fetched just now (not a cached one), and not for a ticker removed meanwhile. Other ranges' previous close means
  something else, so they never touch the saved quote.
- **TalkBack**: the chart has three custom actions, "Previous point", "Next point" and "Back to latest", that move the scrub, and
  the scrubbed point (date, price, change) is its state description. Not tried with TalkBack itself yet.

## Consequences
- The header and the watchlist row can update when a chart is opened (the new price and time), not only on a refresh.
- Supersedes the TalkBack gap in decision 0028.
