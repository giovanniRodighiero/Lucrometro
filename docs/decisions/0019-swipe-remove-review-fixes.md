# 0019: Swipe-to-remove review fixes

Status: accepted (2026-11-18). Amends 0018 (behaviour of undo and removal).

## Context
A medium code review of 0018 found five real problems. Two review points were declined: only the latest
removal can be undone (kept, as in 0018), and the swipe state is not keyed on the ticker code (rows are
already keyed by code in the LazyColumn, so a composition is never reused for another ticker).

## Decision
- **Removal and undo run in the ViewModel's scope** (`viewModelScope.async { ... }.await()`), not the row's.
  Once Room deletes the row the list drops it and cancels the row's scope, which could cancel the removal
  before the undo was set.
- **Restore shifts later rows down**: `putBack` first runs `UPDATE ... SET position = position + 1 WHERE
  position >= :position`, then inserts. A ticker added after a removed last row takes `MAX + 1`, which is the
  removed row's old position; without the shift both rows shared it and their order was undefined. The undone
  row now comes back before such tickers. Positions may have gaps; only their order matters.
- **Refresh skips tickers removed meanwhile**: after each fetch it checks the ticker is still stored, so a
  removed (then undone) ticker is not marked failed/stale. A storage error there is not treated as "removed".
- **A failed undo says so**: `undoRemove` returns whether it worked; if not (code added again, storage
  error) the screen shows "Couldn't restore X" (English and Italian). The undo state is cleared after that
  message, because clearing it changes the effect key and would cancel the message.
- Imports in `WatchlistScreen.kt` sorted.

## Consequences
- Verified on the emulator: remove the last row, add a new ticker, undo: the order is kept, also after a
  restart. Remove, add the same code, undo: the message shows and the list has one entry.
- The DAO's shift is checked on the emulator only; there are no instrumented tests yet (see 0016).
