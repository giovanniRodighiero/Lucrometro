# 0056: The details chart catches up with the header

Status: accepted (2026-11-25)

## Context
The header shows the saved quote, which a watchlist refresh also updates; the chart changed only when it was loaded. So
the chart could end behind the header: on returning to the app with details open (reproduced on the emulator with
SWDA.MI: header 122.98, chart ending at 122.28 after 5 minutes away), or on a range reused from the session cache.

## Decision
- While details is shown, if the saved quote is from a later trade than the chart's response
  (`meta.regularMarketTime`, kept for every range), the selected range is fetched again, past the cache.
- The old chart stays until the answer, without a spinner. If the fetch fails, or brings too few bars to draw, it
  stays too, with no retry.
- Sent after the same 250 ms wait as a range switch, if the range is still selected. One at a time per range, and
  asked once per range and quote time, so an answer that is still behind cannot loop.
- No catch-up for the quote saved when an auto-refresh poll fails: nothing is requested right after a failure.
- Not during auto-refresh: polls update 1D, and reloading another range every 15 s would double the requests. There the
  mismatch stays until auto-refresh stops.

## Consequences
- One more request only when there is a newer trade; none while the market is closed and the chart is up to date.
- A cached range can show for a moment before the newer one replaces it. A scrub is not reset while the finger is down.
