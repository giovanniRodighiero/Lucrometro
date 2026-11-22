# 0041: A broker-neutral trade CSV; the average leaves out fees

Status: accepted (2026-11-22), changes part of 0040

## Context
Decision 0040 starts a trade log (M2) filled once from the broker's history and then by hand after each buy. Two
orders exports from the owner's broker were checked (docs/research/broker-orders-export.md). Replayed with the
weighted average cost, they match the broker's average to 4 decimals **without** fees, and the export has no fee
column. Its columns are specific to that broker and in Italian. The app should not depend on one broker, and the
repo should not reveal which one the owner uses.

## Decision
- **The app imports one format, its own**: the trade CSV below. Each broker's export is turned into it outside the
  app, by a small conversion script. The owner keeps theirs private; no broker-specific code or names go in the repo.
- **Format**
  - UTF-8 (a BOM is allowed), `,` as separator, `.` as decimal mark, no thousands separator. A header row is required;
    columns are matched by name in any order, and unknown columns are ignored. Rows can be in any order.
  - Columns:

| Column | Required | Format | Meaning |
|---|---|---|---|
| `ticker` | yes | `ENI.MI` | The Yahoo symbol, as in the watchlist |
| `side` | yes | `buy` or `sell` | |
| `date` | yes | `2026-02-01` | Day of the trade |
| `time` | no | `10:00` or `10:00:00` | Time of the trade in the exchange's time zone. Empty if unknown |
| `quantity` | yes | `10` or `2.5` | Shares, more than 0 |
| `price` | yes | `50.123456` | Price per share in the ticker's currency, without fees |
| `id` | no | any text | The broker's order or trade number. A row whose id is already saved is skipped, so the same file can be imported again |

  - Example:
    ```
    ticker,side,date,time,quantity,price,id
    ENI.MI,buy,2026-02-01,10:00:00,10,14.25,A123
    ENI.MI,sell,2026-03-15,,4,15.10,
    ```
- **The average** is the weighted average cost **without fees**, which is what the owner's broker shows. A sell keeps
  the average; at 0 shares it starts again. This replaces 0040's "fees included".
- **Checks on import**: a bad row is reported with its line number and nothing is saved. Replaying each ticker oldest
  first must never take the shares below 0, otherwise older trades are missing and the import is refused.
- **Manual entry** uses the same fields, so a typed trade and an imported one are the same thing.
- M2 imports buys **and** sells, so shares and the average are right from the start. Sell screens and realized
  gains stay in M3.

## Consequences
- No fee field for now. A broker that includes fees in its average would show a slightly different number; if that
  matters, an optional `fees` column can be added later without breaking files that don't have it.
- Realized gains (M3) will be without fees too, so they won't match tax statements; M3 must say so or add fees.
- The 1D/5D buy point uses `time` when present (the owner's broker gives the time the order was placed, close enough
  for intraday orders); open for M2 how to draw a trade without a time.
- Open for M2: trades for a ticker not in the watchlist (skip, or add it), and where the import lives in the UI.
