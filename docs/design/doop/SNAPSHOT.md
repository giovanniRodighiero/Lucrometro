# Doop snapshot

Canvas: **Lucrometro**, ID `VPv84853nU`. Exported 2026-11-17 by hand, following `.claude/skills/doop-sync`
(`export`), with the deviations listed below; details and position frames re-exported 2026-11-19. Re-running the export overwrites this folder.

## Frames (app screens)

| Slug | Frame name | Frame ID | Files |
|---|---|---|---|
| `watchlist` | Watchlist | `5aB-G0SvgI` | `frames/watchlist.html`, `.png` |
| `watchlist-dark` | Watchlist (dark) | `xbGMpKekxj` | `frames/watchlist-dark.html`, `.png` |
| `add-ticker` | Add ticker | `qEXVp1o8Vm` | `frames/add-ticker.html`, `.png` |
| `watchlist-ticker-added` | Watchlist (ticker added) | `k50gQ9cJnm` | `frames/watchlist-ticker-added.html`, `.png` |
| `details-1d` | Details · 1D (default) | `l6XKsmWmT4` | `frames/details-1d.html`, `.png` |
| `details-1d-scrub` | Details · 1D scrubbing | `plLyoVrchS` | `frames/details-1d-scrub.html`, `.png` |
| `details-1m-loss` | Details · 1M (loss) | `6kX94i4aE6` | `frames/details-1m-loss.html`, `.png` |
| `details-chart-failed` | Details · chart failed (stale header) | `ysEbtRvZXk` | `frames/details-chart-failed.html`, `.png` |
| `details-1d-dark` | Details · 1D (dark) | `2FJ3ZK7LCY` | `frames/details-1d-dark.html`, `.png` |
| `details-worst-case` | Details · worst case (stress values) | `iJVgK2AL0n` | `frames/details-worst-case.html`, `.png` |
| `details-position` | Details · position (avg + shares) | `zoEYo9D6DA` | `frames/details-position.html`, `.png` |
| `details-position-avg-only` | Details · position (avg only, below the chart) | `QUevA8SJK_` | `frames/details-position-avg-only.html`, `.png` |
| `details-add-position` | Details · add position sheet | `-rhgnhOKIC` | `frames/details-add-position.html`, `.png` |
| `details-edit-position` | Details · edit position sheet | `a02OBvdVC9` | `frames/details-edit-position.html`, `.png` |
| `market-status-cases` | Reference · market status label cases (phone in Italy) | `mcAknly6zH` | `frames/market-status-cases.html`, `.png` |

The details frames (added 2026-11-17, decision 0023) use made-up prices. `details-worst-case` uses stress values
(a 4-digit price and a large drop) to check that the header row fits; it is not a real quote for RACE.MI.

`market-status-cases` (added 2026-11-19, decision 0032) is a reference board, not a screen: the top bar's market
status for exchanges in other zones, seen from a phone in Italy.

The position frames (added 2026-11-19, decisions 0033 and 0034) are marked ready for dev on the canvas. With the
position work every details frame was re-exported: the chart is 292dp (was 324) and the empty `PositionCard` sits
under the range bar. `details-position` shows the average line and the last-buy diamond; `details-position-avg-only`
shows an average below the axis range (edge marker) and no diamond, as its last buy is outside the 1D range.

## Tokens and components

- `tokens.json`: all 66 tokens with light values and the `dark` theme override. The five `--color-icon-*`
  tokens (gold, gold-deep, green, red, coffee) were deleted from the canvas on 2026-11-17: leftovers of
  rejected icon concepts, used by nothing.
- `components/`: `QuoteRow`, `ChangeBadge`, `TopAppBar`, `Fab`, `Button`, `TextField`, `BottomSheet`
  (the app UI), plus `StatusBar` and `Keyboard` (system chrome mocks: Android draws these, do not port).
  Details screen: `DetailsTopBar`, `QuoteHeader`, `ChartReadout`, `PriceChart`, `RangeTab`, `PositionCard`. `PriceChart`'s
  header comment holds the chart geometry; the chart lines in the frames are mock data.
- Style guides: the canvas has none.

## Not exported (deviations from the skill)

- Frame HTML omits the instance ids, the `<style data-doop-tokens>` / webfonts blocks and overrides that equal a
  component default, and writes default props explicitly (e.g. `title="SMH.MI"`): the values are in
  `tokens.json` and the `var(--name)` references are intact. The `QuoteRow` with no props in the frames is
  the component default (`ENI.MI`, `Eni S.p.A.`, `14.86 EUR`).
- The component main frames (`StatusBar`, `TopAppBar`, `ChangeBadge`, `QuoteRow`, `Fab`, `Button`,
  `TextField`, `BottomSheet`, `Keyboard`, and the six details components) are not saved as frames: their markup
  is in `components/`.
- `tokens.json` was not re-exported on 2026-11-17 or 2026-11-19: no token changed (checked on 2026-11-19).
- The watchlist frames were not re-exported on 2026-11-19: unchanged on the canvas since 2026-11-17.
- The page "Exploration · Position" (a rejected chart frame, an undecided watchlist line) is not exported.
- The icon frames (`IconCard`, `LayerCard`, `Icon master`, `Layer 1-3`, `Play Store icon`) are presentation
  boards for the app icon; the artwork lives in `docs/brand/` and `app/src/main/res/` (decisions 0008, 0010).
  The `IconCard` and `LayerCard` components are not saved either.
