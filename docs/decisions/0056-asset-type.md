# 0056: Asset type (ETF or stock) and the breakdown by type

Status: accepted (2026-11-25), refines M5 of 0049

## Context
Decision 0049 scoped M5 as a type per ticker and a breakdown by type on the Portfolio tab. In the M5 brainstorm
(2026-11-25) the owner narrowed it to the type alone, ETF or stock; sector and industry are parked until a later UI
review. The owner checked in another session that Yahoo returns a type for each ticker. Designed in Doop on
2026-11-25, page "M5 · Asset type". Watchlist options drawn: a 40dp icon in a tonal circle before the code, a 16dp
icon after the code, a text tag ("ETF" / "Stock") after the code; for the ETF icon, layers and a pie. For the
Portfolio tab (whose icon was the pie): a piggy bank, a briefcase, banknotes, coins and a euro sign. The six chosen
frames are signed off (snapshot in `docs/design/doop/`).

## Decision
- **Where the type comes from**: read from Yahoo, never typed or edited by the owner. Only two values are shown:
  ETF and stock. Any other type Yahoo reports (index, fund, currency, crypto, ...) gets no icon and stays out of
  "By type"; it can be added later. ETCs and similar products follow whatever Yahoo says.
- **The icon**: 16dp, on-surface-variant, right after the ticker code with a 6dp gap: Material `pie_chart` for an
  ETF (a slice of many things), `business` (a building) for a stock (one company). Content description "ETF" /
  "Stock". It stays quieter than the code and the price. Rejected: the tonal circle (indents every row by 56dp,
  too loud), the text tag (clear but heavier on 10-15 rows), the layers glyph (too close to the building at 16dp).
- **Watchlist**: every row with a known type shows the icon after the code. The year detail list uses the same row.
- **Details**: the name line under the header starts with the icon and the word, "ETF · VanEck Semiconductor UCITS
  ETF", "Stock · Eni S.p.A."; the name still ellipsizes. This is where the icon is explained in words.
- **Portfolio tab icon**: the piggy bank (Material `savings`) replaces the pie in the navigation bar, since the pie
  now means ETF. Outlined when not selected, filled when selected, as Material 3 does. Rejected: briefcase (cold),
  banknotes (cash, not investments), coins (unclear at 24dp), euro (a currency, not a portfolio); the wallet is
  already the public/private switch.
- **"By type" on the Portfolio tab**: a new section between "Growth by year" and "Composition", titled "By type",
  "by value" on the right. One row per type held (ETFs, then Stocks, largest first): icon, label, "N holdings",
  weight in % with one decimal, a bar, and in private the value in EUR. Same row as Composition (`HoldingRow`),
  not tappable. A type with no holdings has no row; with a single type the section shows one row at 100%.
- **Public view**: "By type" is shown as **% only** (no value), even though Composition stays private (0050):
  the split between ETFs and stocks reveals no amounts or tickers.
- **Composition**: each holding shows its type icon after the ticker.

## Consequences
- **Data**: the type is read behind `QuoteSource` (nothing outside `data/` knows the field). The field and the values
  seen for `.MI` ETFs and stocks come from the owner's spike, written in `docs/research/data-sources.md` before the
  build; prefer a field in the chart response the app already fetches (no extra request). The type is saved with the
  ticker in Room (schema bump and migration, CLAUDE.md), filled on the next refresh for tickers already in the list.
- **Weights**: "By type" weights come from the same values as Composition and the total; they are rounded
  separately, so they may not add up to exactly 100.0%.
- **Strings**: "ETF", "Stock" (IT "Azione"), "By type" (IT "Per tipo"), "ETFs" / "Stocks" (IT "ETF" / "Azioni"),
  "N holdings" already exists for the total card. Both `values/` and `values-it/` (0017).
- **Doop**: `QuoteRow` rev 4, `QuoteHeader` rev 7 and `HoldingRow` rev 2 gain `kind` (none|etf|stock, default none);
  `NavBar` rev 6 gains `portfolio-icon` (pie|savings, default pie). The defaults keep the "Screens" page as built
  until M5 merges; then the frames there get their types and the piggy bank, and `portfolio-icon` is dropped.
- **Later**: sector and industry for stocks, category for ETFs; an "Other" type.
