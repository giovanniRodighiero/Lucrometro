# 0014: Details screen layout

Status: accepted (2026-11-17)

## Context
Decision 0013 set the details screen's defaults and baselines and said the screen would be designed in Doop
first. It was designed on the canvas "Lucrometro" on 2026-11-17 (frames `details-*` in `docs/design/doop/`),
with several alternatives tried and dropped along the way.

## Decision
Top to bottom, the screen is:
- **Top app bar**: back arrow, ticker code as title, and the market status on the right, e.g.
  "● Open · closes 17:30" or "○ Closed · opens Mon 09:00". It is exchange-level context, so it sits where the
  watchlist shows its "Updated" stamp and adds no row. It is hidden when it cannot be known reliably (chart fetch
  failed, or no trading-hours data); if the next opening time is uncertain (holidays), show "Market closed".
- **Quote header**: instrument name; the last price on its own row (display size); then one row with the day
  change pill (abs and %) on the left and the stamp on the right edge. The stamp wraps below the pill only when
  both do not fit (large font scale). Chosen over the pill on the price row (inline) and the stacked layout,
  because it looks the same at every font size and price length.
- **Stamp**: the quote's own time, "Today, 14:32", not the fetch time ("Updated 14:32"). It describes the market,
  not the app, and labels the day-change pill. It must come from the quote timestamp (Yahoo
  `regularMarketTime`), or a delayed quote would look fresher than it is. When the market is closed it names the
  session day: "Fri 14 Nov, 17:35". The stale state keeps "Stale since HH:mm" (PRD 4). The watchlist keeps
  "Updated HH:mm", which is about the list refresh.
- **Chart readout** (one line above the chart, same shape in every state so nothing moves):
  - 1D: "Previous close · 40.41 EUR" with no pill: the day change is already in the header (PRD's "range
    summary: change %" is met by the header for 1D).
  - 5D and up: "Past month · From 42.60 EUR" with the range change pill (abs and %).
  - While scrubbing: date/time and price at the finger, and the change from the range start (1D: from the
    previous close). Releasing returns to the summary.
- **Chart**: line with area fill; line colour follows the sign of the selected range's change (gain, loss, or
  neutral when flat). 1D draws the previous close as a dashed baseline and stops the line at the last quote, so
  the rest of the 09:00-17:30 session stays empty. Prices in a right gutter, times or dates on the bottom row.
- **Range bar**: eight equal cells (1D 5D 1M 3M 6M 1Y 5Y Max) that tile the row with no gaps, 48.5 x 48dp
  each; only the selected range gets a pill. The earlier filter chips were about 41dp wide with 6dp dead gaps,
  below the 48dp minimum. Segmented buttons (8 is over Material's 2-5 guidance, cramped), M3 tabs (read as
  navigation), a picker sheet (two taps per switch) and fewer ranges were considered and not chosen.
- **Chart failed**: the header keeps the last saved quote marked "Stale since HH:mm"; no market status; the chart
  area shows "Couldn't fetch" with Retry (decision 0013).

## Consequences
- The range bar is not a stock Material component: a `Row` of selectable cells inside
  `Modifier.selectableGroup()`, each with a role for TalkBack. Supersedes the "FilterChip/SegmentedButton for
  ranges" line in `docs/design/native-look-and-feel.md`.
- New Doop components, snapshotted in `docs/design/doop/components/`: `DetailsTopBar`, `QuoteHeader`,
  `ChartReadout`, `PriceChart`, `RangeTab`. `ChangeBadge` is reused for both pills.
- `QuoteSource` needs, per quote: the quote timestamp, and the exchange's trading period (for market status and
  "closes 17:30"). To verify against Yahoo (roadmap item 1): `regularMarketTime`, `currentTradingPeriod`,
  holiday handling, and the `.MI` intraday delay. If Milan quotes are delayed, the screen must say so.
- Font scaling (130%, 200%) is checked in the app, not in Doop: the stamp must wrap, never overlap or clip.
- Not designed yet: loading state, 1D before the first trade of the day, 5Y/Max with coarse points.
