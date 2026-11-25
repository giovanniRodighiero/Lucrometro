# 0055: Pre-market price on the details screen

Status: accepted (2026-11-25), extends 0023 and 0035

## Context
For US tickers (Nasdaq, NYSE) the price moves before the regular session opens, and the owner wants to see that move
on the details screen before the open. Yahoo returns a pre-market price for US tickers (checked by the owner in
another session); it has none for `.MI` tickers. Designed in Doop on 2026-11-25, page "Exploration · Pre-market".
Three directions were drawn: A, a smaller pre-market column right of the big price; B, the same in a tonal block;
C, a full-width strip under the header. The owner chose C: the header stays as it is, and the strip never collides
with a long price or a large font. The four C frames are signed off (snapshot in `docs/design/doop/`,
`details-premarket*`).

## Decision
- **Only while the market is closed.** The strip shows only when the top bar marker says "Closed" and Yahoo reports
  a pre-market price for the coming session. Once the market opens it is not displayed at all: the header's price is
  then the live price again. It is also hidden whenever there is no pre-market price, so `.MI` tickers never show it.
- **Pre-market only**, not after hours: after the close (US 16:00-20:00 ET) the screen stays as today. A pre-market
  price older than the last regular close (yesterday's, still in the response) is not shown.
- **Where**: `PreMarketStrip`, a full-width tonal line (surface-container-highest, 12dp corners) directly under the
  quote header, above the readout. Left to right: "Pre-market", the price in the quote's decimals without currency
  (the header shows it), the change from the last regular close as arrow + % (gain/loss colour, the arrow so it is
  not colour alone, 0007), and the time of the price on the right, in the exchange clock with its zone ("08:32 EST",
  0031, 0032). At large font sizes the time wraps onto a second line.
- **The header is unchanged**: price, day change pill and stamp keep the last regular session ("Mon 24 Nov, 16:00").
  The 1D chart and the readout are unchanged too: no pre-market bars on the chart.
- **The top bar is unchanged**: the marker stays "○ Closed" during pre-market, and auto-refresh stays disabled as
  when closed (0035). The strip is fetched with the screen's normal load (each opening reloads, 0029).
- **Not tappable.** TalkBack reads it as one item: "Pre-market 184.30, up 0.96%, at 08:32 EST".

## Consequences
- **Data**: the build needs the pre-market price, its time and the market state from Yahoo, behind `QuoteSource`
  (nothing outside `data/` knows the field names). Which request carries them (the chart call already made, or
  one extra) is settled on the build laptop with one request at a time (CLAUDE.md), and written in
  `docs/research/data-sources.md`. Prefer the existing chart call: an extra request per opening doubles the Yahoo
  traffic for US tickers.
- **Not saved**: the pre-market price is not stored in Room and does not reach the watchlist row; it is shown only
  when just fetched. No schema change.
- **Strings**: "Pre-market" in `values/strings.xml`, and in `values-it/strings.xml` too ("Pre-market" is the usual
  Italian term in broker apps), plus the TalkBack sentence (0017).
- **Doop**: new component `PreMarketStrip` (direction up|down|flat, price, change, time). Frames: full screen
  (NVDA, up), down (QQQ), 130% font with a 4-digit price (BKNG), dark. Prices are invented. The A and B frames stay
  on the exploration page until the build merges; then the C frames move to "Screens" row 2 and the page is deleted.
