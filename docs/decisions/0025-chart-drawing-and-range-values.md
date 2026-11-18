# 0025: Chart drawing and what the range values show

Status: accepted (2026-11-18)

## Context
Iteration 3 of the details screen draws the 1M chart (decision 0023) from Yahoo daily candles (decision 0022).

## Decision
- **Canvas, no chart library.** `PriceChart` draws the gridlines, the line, the area fill and the end dot as in the Doop frame.
- **Axis**: gridlines at round prices (1, 2 or 5 times a power of ten) that bracket the data, about 3-4 of them. The x position
  follows the time, not the bar index, so weekends show as the gaps they are. Four date labels (start, two between, end).
- **Values**: the line and the range change use the adjusted close (`adjClose`, falling back to `close` if the source
  gave none). The readout's "From" shows the first bar's plain close, the price you would have seen. They differ only
  when a distribution falls inside the range.
- **Chart request**: one `history` call per opening of the details screen, through the repository's source lock, in the
  ViewModel (cancelled with the screen). There is no cache yet (planned with the range bar).
- **Loading** (not in Doop): a centred progress indicator in the chart area, the readout keeps its height.
  **Failed**: the Doop "Couldn't fetch" card with Retry.

## Consequences
- `Candle` gained `adjClose`. Decision 0022's caveat is resolved (see `docs/research/data-sources.md`).
- 1M is fixed until the range bar (next iteration). The 1D baseline, intraday and scrub are not built.
