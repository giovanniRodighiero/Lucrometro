# Doop snapshot

Canvas: **Lucrometro**, ID `VPv84853nU`. Exported 2026-11-17 by hand, following `.claude/skills/doop-sync`
(`export`), with the deviations listed below; details and position frames re-exported 2026-11-19, and again on
2026-11-20 for the auto-refresh design (decision 0035), and the position frames on 2026-11-22 for M1 (decision 0040). Re-running the export overwrites this folder.

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
| `details-position` | Parked for M2 (private view) · position (avg + shares) | `zoEYo9D6DA` | `frames/details-position.html`, `.png` |
| `details-position-avg-only` | Details · position (avg only, below the chart) | `QUevA8SJK_` | `frames/details-position-avg-only.html`, `.png` |
| `details-add-position` | Details · add position sheet | `-rhgnhOKIC` | `frames/details-add-position.html`, `.png` |
| `details-edit-position` | Details · edit position sheet | `a02OBvdVC9` | `frames/details-edit-position.html`, `.png` |
| `details-auto-refresh-on` | Details · auto-refresh on | `tMJVHoDnfM` | `frames/details-auto-refresh-on.html`, `.png` |
| `details-market-closed` | Details · market closed (auto-refresh disabled) | `LbQyoDJzA4` | `frames/details-market-closed.html`, `.png` |
| `details-market-sheet` | Details · market details sheet | `jpgBg-4wm5` | `frames/details-market-sheet.html`, `.png` |
| `market-status-cases` | Reference · market status line in the sheet (phone in Italy) | `mcAknly6zH` | `frames/market-status-cases.html`, `.png` |
| `auto-refresh-spec` | Reference · auto-refresh behaviour | `gBgQzbEzah` | `frames/auto-refresh-spec.html`, `.png` |

The details frames (added 2026-11-17, decision 0023) use made-up prices. `details-worst-case` uses stress values
(a 4-digit price and a large drop) to check that the header row fits; it is not a real quote for RACE.MI.

`market-status-cases` (added 2026-11-19, decision 0032; reworked 2026-11-20) is a reference board, not a screen: the market
status line for exchanges in other zones, seen from a phone in Italy. Since decision 0035 that line is shown in the
details sheet, not in the top bar. `auto-refresh-spec` is a reference board too: the three signals (marker, icon, halo)
and the polling rules as text.

Auto-refresh (decision 0035, 2026-11-20): `DetailsTopBar` is rev 5 (marker and sync icon instead of the status text), so
every details frame shows the new top bar. The old text-only top bar is in git at 38e73f3. There is no separate "off"
frame: `details-1d` is the off state. The three new screens (`details-auto-refresh-on`, `details-market-closed`,
`details-market-sheet`) are marked ready for dev on the canvas, signed off by the owner on 2026-11-20. The page "Exploration · Auto-refresh" was deleted with its rejected options
(header chip, icon beside the status text, status strip under the header).

The position frames (added 2026-11-19, decisions 0033 and 0034) are marked ready for dev on the canvas. With the
position work every details frame was re-exported: the chart is 292dp (was 324) and the empty `PositionCard` sits
under the range bar. `details-position` shows the average line inside the range; `details-position-avg-only` shows an
average below the axis range (edge marker).

M1, average only (decision 0040, 2026-11-22): `PositionCard` is rev 4, and its default state is now `avg`. The avg
state shows the average and "Since avg" only (no shares hint, no "Last buy" footer). The add and edit sheets ask for the
average only, and no frame draws the last-buy diamond any more. Both sheets were signed off again by the owner on
2026-11-22. `details-position` is **parked for M2**: it keeps the full card (`state="full"`), which M2 will redesign, and
is not to be built in M1. Re-exported: `PositionCard`, `PriceChart` (header comment only), `details-add-position`,
`details-edit-position`, `details-position-avg-only` and `details-position`.

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
