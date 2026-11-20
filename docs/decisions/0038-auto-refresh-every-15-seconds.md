# 0038: Auto-refresh every 15 seconds

Status: accepted (2026-11-20), supersedes the 30 s interval of 0035 (and its request budget)

## Context
The owner asked for a shorter interval than the 30 s of decision 0035.

## Decision
- Auto-refresh polls every 15 s, still counted from the end of the previous fetch, one request at a time, with the same
  stop rules (0035, 0037). The sheet says "Every 15 s".

## Consequences
- 4 requests a minute for one open ticker while it runs (was 2). Still well above the 5-10 s spacing of the live-request
  rules in CLAUDE.md, but Yahoo is unofficial and its 429s are not understood: a 429 stops polling (0035).
- Yahoo's `.MI` quotes are about 15 minutes delayed and the 1D bars are 5 minutes, so a 15 s poll often brings the same
  price; it shortens the wait for the next delayed trade, not the delay.
- The Doop reference board `frames/auto-refresh-spec` still says 30 s until the canvas is updated.
