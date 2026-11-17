# Doop snapshot

Canvas: **Lucrometro**, ID `VPv84853nU`. Exported 2026-11-17 by hand, following `.claude/skills/doop-sync`
(`export`), with the deviations listed below. Re-running the export overwrites this folder.

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

The details frames (added 2026-11-17, decision 0014) use made-up prices. `details-worst-case` uses stress values
(a 4-digit price and a large drop) to check that the header row fits; it is not a real quote for RACE.MI.

## Tokens and components

- `tokens.json`: all 66 tokens with light values and the `dark` theme override. The five `--color-icon-*`
  tokens (gold, gold-deep, green, red, coffee) were deleted from the canvas on 2026-11-17: leftovers of
  rejected icon concepts, used by nothing.
- `components/`: `QuoteRow`, `ChangeBadge`, `TopAppBar`, `Fab`, `Button`, `TextField`, `BottomSheet`
  (the app UI), plus `StatusBar` and `Keyboard` (system chrome mocks: Android draws these, do not port).
  Details screen: `DetailsTopBar`, `QuoteHeader`, `ChartReadout`, `PriceChart`, `RangeTab`. `PriceChart`'s
  header comment holds the chart geometry; the chart lines in the frames are mock data.
- Style guides: the canvas has none.

## Not exported (deviations from the skill)

- Frame HTML omits the instance ids and the `<style data-doop-tokens>` / webfonts blocks: the values are in
  `tokens.json` and the `var(--name)` references are intact. The `QuoteRow` with no props in the frames is
  the component default (`ENI.MI`, `Eni S.p.A.`, `14.86 EUR`).
- The component main frames (`StatusBar`, `TopAppBar`, `ChangeBadge`, `QuoteRow`, `Fab`, `Button`,
  `TextField`, `BottomSheet`, `Keyboard`, and the five details components) are not saved as frames: their markup
  is in `components/`.
- `tokens.json` was not re-exported on 2026-11-17 for the details screens: no token changed.
- The icon frames (`IconCard`, `LayerCard`, `Icon master`, `Layer 1-3`, `Play Store icon`) are presentation
  boards for the app icon; the artwork lives in `docs/brand/` and `app/src/main/res/` (decisions 0008, 0010).
  The `IconCard` and `LayerCard` components are not saved either.
