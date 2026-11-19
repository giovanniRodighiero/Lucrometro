# 0034: The last buy is a diamond on the chart

Status: accepted (2026-11-19), adds to 0033

## Context
Decision 0033 shows the average price on the chart but not when the owner last bought. Tried in Doop on the
"Exploration · Position" page: a hollow ring, a diamond with a dotted drop line to the time axis, a banknote badge
(illegible at chart size) and a euro badge (only fits EUR tickers). The owner picked the diamond, but the drop line
cut through the chart, and a variant with only a tick on the axis was also set aside.

## Decision
- The last buy is a 15dp diamond in the tertiary colour (the average line's colour), with a 2dp surface outline, on
  the price line at the buy's date.
- Only when that date is inside the selected range (the 1D chart of the "avg only" frame has none: the buy is older).
- No drop line, no axis tick, no date label: the exact date is in the `PositionCard` footer ("Last buy 28 Oct 2026").
- The chart's content description names it ("Your last buy on 28 Oct").

## Consequences
- `PriceChart` draws one more mark; it needs the last buy's date and the price at that point of the line.
- Reading the exact date from the chart is a later idea: show it when a tap or the scrub reaches the diamond, as the
  scrub already shows the price at a point (roadmap).
