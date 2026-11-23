# 0045: M2 build choices

Status: accepted (2026-11-23), fills in what 0041 and 0042 left to the build

## Context
M2 (trade log and private view, decisions 0040-0042) was built on branch `trade-log`. A few choices came up that the
design did not settle.

## Decision
- **Private view starts public** on every launch. It survives rotation and the screens (the switch lives above the
  back stack), not a restart.
- **Trades without a time** are placed in their day so a complete history never looks short: an untimed buy counts at
  the start of its day, an untimed sell at its end. Same-day trades with times follow their times.
- **The shares check runs as you type** in the sheet (the replay is local and cheap), and again in the repository when
  saving, so a change from elsewhere can't slip through. A delete is checked the same way.
- **Averages**: one computed from trades is shown in the quote's decimals ("37.54 EUR"), like a price; a typed one is
  still shown as typed (0036).
- **At 0 shares** M2 already follows 0043: the public card is hidden, the private card says "No shares held" with the
  footer, the chart has no average and no last buy. Realized gains wait for M3.
- **Last buy on the chart**: on daily and longer ranges an untimed buy is placed at its day; on its last day it shares
  the last bar's x, and the scrub prefers the buy there (the last price is already in the header). The readout's pill
  is the change from the buy to now.
- **CSV import**: fields may be quoted (`"41,20"` then reads as one bad price, not two columns); all rows are checked,
  for every ticker, so a broken file is reported even if the bad row is for another ticker; a repeated id inside the
  file counts as already saved. The picker offers `text/*`, `application/csv` and `application/vnd.ms-excel` (how
  some apps label CSV); files over 2 MB are refused unread.

## Consequences
- Room v6 adds the `trade` table (auto-migration); removing a ticker takes its trades, undo brings them back.
- Untested: TalkBack on the new screens, the Italian strings on a device, the diamond on 1D and 5D with live data,
  and the picker with files from other apps (Drive, a file manager).
