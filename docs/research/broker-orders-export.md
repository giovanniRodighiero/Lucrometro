# Rebuilding a position from a broker's orders export (2026-11-22)

Can a broker's CSV of past orders rebuild the position the broker shows (decision 0040, M2)? Checked on two of the
owner's exports, one with buys only and one with buys and sells. The files and the conversion script stay on the
owner's computer (`broker-exports/`, ignored by git); no broker name, orders, amounts or holdings are recorded here.

## What the export had
- One row per order: instrument name, ticker without the exchange suffix (padded with spaces), ISIN, ordered and
  executed quantity, buy or sell, status, limit price, average execution price (up to 6 decimals when an order filled
  at several prices), order type and expiry, the date and time the order was **placed**, and a unique order number.
- Semicolon-separated, comma decimals, Italian headers, UTF-8 with a BOM, newest first.
- Not in it: commissions, taxes, currency, exchange, dividends, splits.
- All orders seen were executed, intraday limit orders.

## Result
- Replaying executed orders oldest first with the weighted average cost (a sell keeps the average, 0 shares resets
  it) gives **exactly** the average the broker shows, to 4 decimals, for both files. So that broker's average leaves
  out commissions, and the export alone is enough.
- The reset at 0 shares matters: one file sells out completely before the current position starts.
- Neither file takes the shares below 0 when replayed, so both are complete histories.
- Sells have the same row shape as buys.

These findings shaped the app's own trade CSV (decision 0041).

## Open questions
- Other order statuses (cancelled, partial, expired) and order types (market, stop, multi-day): none in these files.
- Whether Italian shares' Tobin tax changes the average; whether one export can hold several tickers.
- How splits and other corporate actions appear.
