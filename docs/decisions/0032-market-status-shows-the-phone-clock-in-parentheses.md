# 0032: The market status also shows the phone's clock

Status: accepted (2026-11-19), refines 0031 for the market status label only

## Context
Decision 0031 shows market events in the exchange's clock. For a Nasdaq ticker on a phone in Italy the label read
"Closed · opens 09:30", which looked wrong (it is 15:30 in Milan): the time was correct but had no zone.

## Decision
- The market status label ("Open · closes ...", "Closed · opens ...") keeps the exchange's clock and adds the same
  moment on the phone's clock in parentheses: "Closed · opens 09:30 EST (15:30)". When the phone's day differs the
  weekday is added: "(Mon 01:00)".
- The exchange's time carries its zone name (EST, CET, JST); the phone's time in parentheses does not, it is taken
  for granted. (Added after the first try, without the name, was confusing on a Nebius check.)
- The weekday ("opens Fri 09:30") is shown only when the opening is not tomorrow in the exchange's calendar (Friday
  evening and the weekend); otherwise it is "opens 09:30".
- A long symbol in the top bar is cut with an ellipsis; the label stays on one line and keeps its width.
- Zones without a short name come as an offset, shortened ("GMT+8").
- Nothing is added when both clocks read the same (phone in the exchange's zone, e.g. Milan tickers in Italy).
- Everything else (chart axis, scrub label, trade stamps) stays as in 0031.

## Consequences
- The label is longer for foreign exchanges; check it fits on narrow screens.
- Computed from the real instant, so the US/Europe DST gap (a few weeks a year) shows the right local time.
