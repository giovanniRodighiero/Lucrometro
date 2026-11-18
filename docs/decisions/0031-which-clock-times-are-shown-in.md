# 0031: Which clock times are shown in

Status: accepted (2026-11-18)

## Context
Times were formatted in four places, and they disagreed about the time zone. The chart axis, the scrub label and the
market status used the exchange's zone (from the chart response); the details header showed the last trade's time
in the phone's zone. On a phone set to another zone (e.g. New York, 6 hours behind Milan) the same trade read
"Today, 11:30" in the header and "Today, 17:30" on the chart's last point. In Italy both zones are the same, so it
never showed.

## Decision
- **Market events** (a trade, a bar, a session's hours) are shown in the **exchange's** clock: "closes 17:30" is what
  the exchange says, wherever the phone is. The "Today" of a market stamp is the exchange's today.
- **Phone events** (when a quote was fetched: "Updated", "Stale since") are shown in the **phone's** clock: they
  describe something the phone did.
- All formatting goes through `ui/common/TimeFormats` (`marketStamp`, `phoneStamp`, and patterns by skeleton), so the
  rule lives in one place. The 12/24h choice still follows the phone setting.
- The quote carries the exchange's zone (`exchangeTimezoneName` from Yahoo), stored with the saved quote: schema v3.
  Quotes saved before v3 have none until their next fetch; until then the phone's zone is used.

## Consequences
- Nothing changes for a phone in the exchange's zone.
- Times are not labelled with a zone. If that confuses a traveller, add the zone to the stamp later.
