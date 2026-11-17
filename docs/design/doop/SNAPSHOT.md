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

## Tokens and components

- `tokens.json`: all 66 tokens with light values and the `dark` theme override. The five `--color-icon-*`
  tokens (gold, gold-deep, green, red, coffee) were deleted from the canvas on 2026-11-17: leftovers of
  rejected icon concepts, used by nothing.
- `components/`: `QuoteRow`, `ChangeBadge`, `TopAppBar`, `Fab`, `Button`, `TextField`, `BottomSheet`
  (the app UI), plus `StatusBar` and `Keyboard` (system chrome mocks: Android draws these, do not port).
- Style guides: the canvas has none.

## Not exported (deviations from the skill)

- Frame HTML omits the instance ids and the `<style data-doop-tokens>` / webfonts blocks: the values are in
  `tokens.json` and the `var(--name)` references are intact. The `QuoteRow` with no props in the frames is
  the component default (`ENI.MI`, `Eni S.p.A.`, `14.86 EUR`).
- The component main frames (`StatusBar`, `TopAppBar`, `ChangeBadge`, `QuoteRow`, `Fab`, `Button`,
  `TextField`, `BottomSheet`, `Keyboard`) are not saved as frames: their markup is in `components/`.
- The icon frames (`IconCard`, `LayerCard`, `Icon master`, `Layer 1-3`, `Play Store icon`) are presentation
  boards for the app icon; the artwork lives in `docs/brand/` and `app/src/main/res/` (decisions 0008, 0010).
  The `IconCard` and `LayerCard` components are not saved either.
