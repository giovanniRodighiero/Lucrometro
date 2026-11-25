# Doop snapshot

Canvas: **Lucrometro**, ID `VPv84853nU`. Exported 2026-11-17 by hand, following `.claude/skills/doop-sync`
(`export`), with the deviations listed below; details and position frames re-exported 2026-11-19, and again on
2026-11-20 for the auto-refresh design (decision 0035), and the position frames on 2026-11-22 for M1 (decision 0040); the M2 private view added on 2026-11-22 (decision 0042). On 2026-11-23, with M2 built, `TopAppBar` and `DetailsTopBar` default to `privacy="public"`, so every older screen shows the wallet; their PNGs were re-rendered (HTML unchanged), and the M2 frames moved to the "Screens" page. The M3 realized gains added on 2026-11-23 (decisions 0043, 0047), on branch `realized-gains`. The M4 portfolio overview added on 2026-11-24 (decisions 0049, 0050), on branch `portfolio-overview`. The global CSV import added on 2026-11-24 (decision 0054), on branch `global-import`. The pre-market strip added on 2026-11-25 (decision 0055), on branch `premarket`. The M5 asset type added on 2026-11-25 (decision 0056), on branch `asset-types`. Re-running the export overwrites this folder.

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
| `details-position-avg-only` | Details · position (avg only, below the chart) | `QUevA8SJK_` | `frames/details-position-avg-only.html`, `.png` |
| `details-add-position` | Details · add position sheet | `-rhgnhOKIC` | `frames/details-add-position.html`, `.png` |
| `details-edit-position` | Details · edit position sheet | `a02OBvdVC9` | `frames/details-edit-position.html`, `.png` |
| `details-auto-refresh-on` | Details · auto-refresh on | `tMJVHoDnfM` | `frames/details-auto-refresh-on.html`, `.png` |
| `details-market-closed` | Details · market closed (auto-refresh disabled) | `LbQyoDJzA4` | `frames/details-market-closed.html`, `.png` |
| `details-market-sheet` | Details · market details sheet | `jpgBg-4wm5` | `frames/details-market-sheet.html`, `.png` |
| `market-status-cases` | Reference · market status line in the sheet (phone in Italy) | `mcAknly6zH` | `frames/market-status-cases.html`, `.png` |
| `auto-refresh-spec` | Reference · auto-refresh behaviour | `gBgQzbEzah` | `frames/auto-refresh-spec.html`, `.png` |
| `watchlist-public-wallet` | Watchlist · public (wallet) | `iORnDv57Z2` | `frames/watchlist-public-wallet.html`, `.png` |
| `watchlist-private` | Watchlist · private | `4AM60XAPY0` | `frames/watchlist-private.html`, `.png` |
| `private-switch-top-bar-states` | Reference · details top bar with the wallet, every state | `R460h3wG5h` | `frames/private-switch-top-bar-states.html`, `.png` |
| `details-private-auto-refresh` | Details · private, auto-refresh on | `C7Z2AN4HaE` | `frames/details-private-auto-refresh.html`, `.png` |
| `position-card-states` | Position card · all states (public and private) | `U8Ay5Xrjlo` | `frames/position-card-states.html`, `.png` |
| `details-private-1m-last-buy` | Details · private · 1M, last buy on the chart | `jDyQyMRL5k` | `frames/details-private-1m-last-buy.html`, `.png` |
| `details-private-scrub-last-buy` | Details · private · scrub reaches the last buy | `sZlDYER2VQ` | `frames/details-private-scrub-last-buy.html`, `.png` |
| `details-private-5d-last-buy` | Details · private · 5D, last buy at its time | `bkdtfQEFfy` | `frames/details-private-5d-last-buy.html`, `.png` |
| `trades-list` | Trades · list | `3SMBS1vYOJ` | `frames/trades-list.html`, `.png` |
| `trades-deleted-undo` | Trades · after a swipe delete, with undo | `nQ2bd3-E4p` | `frames/trades-deleted-undo.html`, `.png` |
| `trades-empty` | Trades · empty | `3l-dzV_LVE` | `frames/trades-empty.html`, `.png` |
| `trades-add-sheet` | Trades · add trade sheet | `Ik1aembmFN` | `frames/trades-add-sheet.html`, `.png` |
| `trades-edit-sheet-error` | Trades · edit trade sheet (sell too large) | `9jZvW1sgjl` | `frames/trades-edit-sheet-error.html`, `.png` |
| `import-confirm` | Import · confirm before saving | `VaiXMpV6A0` | `frames/import-confirm.html`, `.png` |
| `import-bad-row` | Import · bad row, nothing saved | `vRFrvO0WbE` | `frames/import-bad-row.html`, `.png` |
| `position-card-realized` | Position card · realized gains and sold out (M3) | `VUb6rteCbm` | `frames/position-card-realized.html`, `.png` |
| `trades-realized-sells` | Trades · sells show the realized gain | `AyURtYGfmc` | `frames/trades-realized-sells.html`, `.png` |
| `trades-sold-out` | Trades · sold out | `6k5rsPbLxy` | `frames/trades-sold-out.html`, `.png` |
| `details-private-sold-out` | Details · private · sold out | `g3yE28DVlL` | `frames/details-private-sold-out.html`, `.png` |
| `details-public-sold-out` | Details · public · sold out (no card) | `CCRLGTfVOW` | `frames/details-public-sold-out.html`, `.png` |
| `trades-sell-sheet-realized` | Trades · sell sheet previews the realized gain | `jTYKq6gI_g` | `frames/trades-sell-sheet-realized.html`, `.png` |
| `details-private-worst-case-compact` | Details · private · worst case, compact card fits | `pSW0q1c8cm` | `frames/details-private-worst-case-compact.html`, `.png` |
| `position-card-font-130` | Font size 130% · compact vs regular card | `EccC2JjRax` | `frames/position-card-font-130.html`, `.png` |
| `portfolio-private` | Portfolio · private | `Fdmktgb6q3` | `frames/portfolio-private.html`, `.png` |
| `portfolio-public` | Portfolio · public (% only, no composition) | `TgalT-zMTl` | `frames/portfolio-public.html`, `.png` |
| `portfolio-private-end` | Portfolio · private, scrolled to the end (not counted, realized as one quiet line) | `uNk56JYgbR` | `frames/portfolio-private-end.html`, `.png` |
| `watchlist-private-nav` | Watchlist · private, with the nav bar | `L9Ae6FrvmT` | `frames/watchlist-private-nav.html`, `.png` |
| `portfolio-year-detail` | Year detail · 2025 (yours, then each ETF's market return) | `Fv5w7QJ3B_` | `frames/portfolio-year-detail.html`, `.png` |
| `portfolio-stale` | Portfolio · private, stale prices in the total | `-fRrvVmSll` | `frames/portfolio-stale.html`, `.png` |
| `portfolio-dark` | Portfolio · private (dark) | `rKYy57nDYv` | `frames/portfolio-dark.html`, `.png` |
| `portfolio-font-130` | Portfolio · private, font 130%, worst case (6-figure total) | `BFc74QOpvX` | `frames/portfolio-font-130.html`, `.png` |
| `portfolio-360dp` | Portfolio · private on a 360dp phone (total card fits) | `N3CkdgAjZg` | `frames/portfolio-360dp.html`, `.png` |
| `global-import-top-bar` | Global import · icon in the Portfolio top bar (private only) | `Lc_wkl_c9P` | `frames/global-import-top-bar.html`, `.png` |
| `global-import-empty` | Portfolio · private, empty (no trades yet, Import button) | `8fuFq_dzOK` | `frames/global-import-empty.html`, `.png` |
| `global-import-review` | Global import · review before saving (full screen) | `eNcs46xyp3` | `frames/global-import-review.html`, `.png` |
| `global-import-below-zero` | Global import · one ticker below 0, nothing saved | `P10BC2j3dq` | `frames/global-import-below-zero.html`, `.png` |
| `global-import-saved` | Global import · saved, back on Portfolio with a snackbar | `lXlzEEnKyu` | `frames/global-import-saved.html`, `.png` |
| `details-premarket` | Details · pre-market, before the open (NVDA, up) | `GHDZJy_qdx` | `frames/details-premarket.html`, `.png` |
| `details-premarket-down` | Details · pre-market down (QQQ) | `iEKZ0k377t` | `frames/details-premarket-down.html`, `.png` |
| `details-premarket-font-130` | Details · pre-market, font 130%, worst case (BKNG, 4-digit price) | `Erlfs5u2Qp` | `frames/details-premarket-font-130.html`, `.png` |
| `details-premarket-dark` | Details · pre-market (dark) | `BQ2xAJx5AN` | `frames/details-premarket-dark.html`, `.png` |
| `watchlist-type-icons` | Watchlist · type icon after the code (pie = ETF, building = stock), piggy bank tab | `BZbLjePuME` | `frames/watchlist-type-icons.html`, `.png` |
| `nav-bar-piggy-bank` | Nav bar · piggy bank for Portfolio, both tabs selected | `WM_EvcwNGO` | `frames/nav-bar-piggy-bank.html`, `.png` |
| `details-type-etf` | Details · ETF (type in the name line) | `gCyU_z-KH_` | `frames/details-type-etf.html`, `.png` |
| `details-type-stock` | Details · stock (type in the name line) | `FPAt9DZC7C` | `frames/details-type-stock.html`, `.png` |
| `portfolio-by-type` | Portfolio · private, full length: By type section + icons in Composition | `aOxCHptl18` | `frames/portfolio-by-type.html`, `.png` |
| `portfolio-public-by-type` | Portfolio · public, By type as % only | `5cgJ7xMp-_` | `frames/portfolio-public-by-type.html`, `.png` |

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
under the range bar. `details-position` (deleted 2026-11-22) showed the average line inside the range; `details-position-avg-only` shows an
average below the axis range (edge marker).

M1, average only (decision 0040, 2026-11-22): `PositionCard` is rev 4, and its default state is now `avg`. The avg
state shows the average and "Since avg" only (no shares hint, no "Last buy" footer). The add and edit sheets ask for the
average only, and no frame draws the last-buy diamond any more. Both sheets were signed off again by the owner on
2026-11-22. The full card frame that was parked for M2 was deleted when M2 was designed (see below). `details-position-avg-only` was signed off again too. Re-exported: `PositionCard`, `PriceChart` (header comment only), `details-add-position`,
`details-edit-position` and `details-position-avg-only`.

M2, private view and per-ticker trades (decision 0042, 2026-11-22): pages "M2 · Private switch" and "M2 · Private view".
All fifteen M2 frames are marked ready for dev, signed off by the owner on 2026-11-22. The wallet switch is the
`privacy` prop of `DetailsTopBar` (rev 12) and `TopAppBar` (rev 7); frames without it (`none`) show the app before M2.
`PositionCard` is rev 8 with the private states (`state="full"` is gone); `position-card-states` is a board of all six.
`PriceChart`'s header describes the last-buy diamond rules. New components: `TradeRow`, `TradesTopBar`,
`SegmentedButton`, `Dialog`; `TextField` is rev 3 (error state, calendar and clock icons). `private-switch-top-bar-states`
is a reference board. The import's other error texts (no rows for the ticker, shares below 0, missing columns, nothing
new) are in decision 0042, not drawn. The frame HTML of the M2 screens was written from the markup sent to Doop, with
the same normalisation as below. The exploration frames (eye, person and lock icons; a bottom navigation bar) were
deleted from the canvas.

M3, realized gains (decisions 0043 and 0047, 2026-11-23): page "M3 · Realized gains". All eight M3 frames are marked
ready for dev, signed off by the owner on 2026-11-23. `PositionCard` is rev 10: state `sold-out`, prop `realized`
(hidden|shown) with a `realized` slot, and prop `layout` (regular|compact). M3 merged on 2026-11-23 (rev 11): compact is
the default, so every private card in the frames is compact, and the M2 private frames' PNGs were re-rendered
(their HTML is unchanged); only `position-card-font-130` keeps a `layout="regular"` card, for comparison. The M3 frames
moved to the "Screens" page and the M3 page was deleted. Change amounts from 10,000 up drop the decimals (decision
0048); no frame shows one. `TradeRow` is
rev 3: prop `realized` (off|on) and a `realized` slot, which replace the amount on sell rows. The example history is
self-consistent (weighted average, replayed): 95 shares at 36.72 EUR, realized +139.76; sold out, +631.60.
`details-private-worst-case-compact` uses stress values (1,250.5 shares, 51,608.14 EUR) on the 1M chart of the M2 frame.
`position-card-font-130` is a reference board: the wrappers scale the text tokens by 1.3 to simulate the system font
size. The rejected frame (worst case with the regular layout, the footer below the screen) moved to "Exploration ·
Position", not exported. The M3 frame HTML was written from the markup sent to Doop and from the M2 snapshot frames it edits.

M4, portfolio overview (decisions 0049 and 0050, 2026-11-24): page "M4 · Portfolio". All nine M4 frames are marked
ready for dev, signed off by the owner on 2026-11-24. New components: `NavBar` (Watchlist, Portfolio; the FAB moves
above it), `PortfolioSummary` (rev 3: view private|public, freshness fresh|stale; "Since avg" renamed "Since you
bought"; its `realized` slot is retired and hidden), `HoldingRow` (one primary bar per ticker instead of a donut, so
dynamic colour keeps working) and `YearGrowthRow`. M4 merged on 2026-11-24 (release 0.4): `PositionCard` is rev 12 and
says "Since you bought" like the app (0050); the PNGs of the nine frames whose card shows the label were re-rendered
(their HTML is unchanged; the board captions of `position-card-realized` and `position-card-font-130` still say "Since
avg", left alone to keep their sign-off). The M4 frames moved to the "Screens" page as row 9 "Portfolio" and the M4
page was deleted. The example numbers are invented and
self-consistent: the yearly amounts (+1,650.20, +3,980.70, +1,490.40) add up to the gain since you bought (+5,249.20)
plus the realized gains (+1,872.10); the 2025 detail adds up (26,300.00 + 6,200.00 + 3,980.70 = 36,480.70).
The holdings' weights are rounded to one decimal and sum to 99.9%. `portfolio-font-130` scales the text tokens by 1.3
like `position-card-font-130`; with rev 3 the "Since you bought" badge wrapped there (see below).
The frame HTML was written from the markup sent to Doop.
After the build (2026-11-24, decision 0053): the "Since you bought" badge wrapped on the owner's phone, so
`PortfolioSummary` is rev 4: today's change and the change since you bought are full-width lines (badge, then label),
and "Invested" is one line below the divider. The PNGs of the five frames that use it were re-rendered (their HTML is
unchanged); `portfolio-360dp` (360x800, signed off) checks worst-case values on a narrow phone. At 130% the badges no
longer wrap either.

Global CSV import (decision 0054, 2026-11-24): page "Global import". All five frames are marked ready for dev, signed
off by the owner on 2026-11-24. `TopAppBar` is rev 11: prop `action` (none|import, default none) puts the import icon
before the wallet, used only on the private Portfolio tab. New component `ImportTickerRow` (kind import|skipped, note
hidden|shown). The review's header is plain markup (a Material 3 full-screen dialog header), not a component.
`global-import-empty` supersedes `portfolio-empty`. The example file is invented; the frame HTML was written
from the markup sent to Doop. Rejected options (an overflow menu, a button at the end of the list) were deleted.
Merged into main 2026-11-24: the empty frame replaced `portfolio-empty` (`aJhrd2BqsW`, deleted, files removed) in
row 9 "Portfolio" of "Screens" and was renamed; the other four were appended to row 8 "Import" (its label note now
covers both imports). The "Global import" page was deleted. Frame HTML unchanged.

## Tokens and components

- `tokens.json`: all 66 tokens with light values and the `dark` theme override. The five `--color-icon-*`
  tokens (gold, gold-deep, green, red, coffee) were deleted from the canvas on 2026-11-17: leftovers of
  rejected icon concepts, used by nothing.
- `components/`: `QuoteRow`, `ChangeBadge`, `TopAppBar`, `Fab`, `Button`, `TextField`, `BottomSheet`
  (the app UI), plus `StatusBar` and `Keyboard` (system chrome mocks: Android draws these, do not port).
  Details screen: `DetailsTopBar`, `QuoteHeader`, `ChartReadout`, `PriceChart`, `RangeTab`, `PositionCard`. Trades (M2):
  `TradeRow`, `TradesTopBar`, `SegmentedButton`, `Dialog`. Portfolio (M4): `NavBar`, `PortfolioSummary`, `HoldingRow`,
  `YearGrowthRow`. Global import (0054): `ImportTickerRow`. `PriceChart`'s
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

Pre-market price (decision 0055, 2026-11-25): built and merged into main 2026-11-25; the four frames (signed off by the owner, ready for dev)
moved to "Screens" row 2 on 2026-11-25 and the exploration page was deleted. New component `PreMarketStrip` (direction up|down|flat). Three of the
frames are crops of the top of the details screen (412 wide, 340 or 440 tall), not full screens; `details-premarket`
is the full screen, its 1D line the `details-1d` path stretched to the whole session. `details-premarket-font-130`
scales the text tokens by 1.3 like `portfolio-font-130`. Prices are invented. The rejected directions (A, a column
right of the price; B, a tonal block there) and the first C sketch were deleted with the page. The frame HTML was
written from the markup sent to Doop.

M5, asset type (decision 0056, 2026-11-25): page "M5 · Asset type". All six frames are marked ready for dev, signed
off by the owner on 2026-11-25. `QuoteRow` rev 4, `QuoteHeader` rev 7 and `HoldingRow` rev 2 have a `kind` prop
(none|etf|stock, default none: the 16dp pie or building icon; in `QuoteHeader` the icon and the word start the name
line); `NavBar` rev 6 has `portfolio-icon` (pie|savings, default pie). The defaults leave every older frame as built;
when M5 merges, the "Screens" frames get their types and the piggy bank, and `portfolio-icon` goes. The two details
frames are crops of the top of the screen (412x252). `portfolio-by-type` is the whole scrolled screen (412x1420), with
the new "By type" section; the holdings use the M4 total (42,899.20) split into five invented values that add up to it.
`nav-bar-piggy-bank` is a reference board. Rejected options (a tonal circle before the code, a text tag, the layers
glyph; briefcase, banknotes, coins and euro for the tab) were deleted. The frame HTML was written from the markup sent
to Doop.
