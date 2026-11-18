# 0028: Touch scrub on the chart

Status: accepted (2026-11-18)

## Context
PRD 4 and decision 0023: dragging a finger along the chart shows the date and price at that point and the change from the
start of the range; releasing returns to the summary. Designed in Doop as `details-1d-scrub`.

## Decision
- **Gesture**: a horizontal drag on the chart (`awaitHorizontalTouchSlopOrCancellation` + `horizontalDrag`). A vertical drag is
  left to the page scroll, which the screen needs at large font scales. A plain tap does nothing. The point under the finger is
  the nearest one by x (clamped to the first and last), so the finger can leave the plot sideways.
- **While scrubbing**: the readout is in scrub mode (label in the primary colour: the date and time, "Today, 11:45" or the day
  and time for intraday ranges, the date for the others; the price in medium weight; the change pill from the range start, for 1D
  from the previous close). The chart draws a 1dp line and a 6dp dot at the point and hides the end dot. The readout keeps its 56dp.
- **Values** are the plotted ones: adjusted closes for 1M and up (decision 0022), plain closes for the intraday ranges.
- The scrub is forgotten when the range or the data changes.

## Consequences
- Not reachable with TalkBack (a drag): the chart's description already gives the range, start, end and change. A non-gesture way to
  step through points would be a later accessibility item.
- No haptic tick and no tap-to-select yet.
