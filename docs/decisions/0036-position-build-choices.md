# 0036: Personal position, choices made in the build

Status: accepted (2026-11-20), adds to 0033 and 0034

## Context
Decision 0033 left the date picker, the numeric keyboard and the validation messages undesigned, and some behaviour
showed up only on the emulator.

## Decision
- **Storage**: Room v4 adds a `position` table (autoincrement id, code, average, shares, buy day as epoch day, saved-at),
  via a Room `AutoMigration` from v3 (only a new table). The latest entry by id is the position. A ticker's entries are
  deleted in the same transaction as its watchlist row and come back with the undo; a save for a code that is not
  stored is rejected.
- **Sheet**: the edit sheet starts with empty fields and "Was ..." below them, as in the frame; the date defaults to
  today. The date field opens the stock Material `DatePickerDialog`, with days after today disabled (a buy is never
  in the future). Number fields use the decimal keyboard. Save is disabled while the average is empty.
- **Validation**, shown below the field as in the add-ticker sheet, on Save: the average must be a number above 0, the
  shares (when given) too. A dot or a comma is the decimal mark; nothing else is accepted (no grouping, no sign, no
  exponent): "1.234,56" is rejected rather than guessed. A storage failure shows "Couldn't save" under the average.
- **Average shown as typed**: at least the quote's decimals and up to 6 more, so a broker average of 29.5123 is not
  rounded to 29.51 in the card, the chart gutter or the "Was" hint.
- **Change since average**: per share with the average only, for the whole position with shares; the percent is the
  same.
- **Last-buy point**: the last point on or before the buy's day in the exchange's zone (a weekend buy lands on Friday,
  a weekly or monthly bar holds its days). A buy today lands on the last point, so the diamond is drawn above the end
  dot (and below the scrub marker), otherwise the dot hides it.
- **Gutter labels**: a gridline label is skipped within 16dp (was 14dp) of the baseline's or the average's label: at 14dp
  they touched.
- The watchlist line ("Avg 29.50 · +39.90%") is not built (owner, 2026-11-20).

## Consequences
- `parseDecimal` has a unit test (`ParseDecimalTest`): it is where typed input enters the data.
- No migration test: the v3 to v4 upgrade was checked on the emulator over an existing v3 database.
