# 0018: Swipe to remove with undo

Status: accepted (2026-11-17)

## Context
PRD 3: remove a ticker by swipe or a row action, with undo in a snackbar; removing also drops its saved
quote. The Doop frames have no remove state.

## Decision
- **Gesture**: Material 3 `SwipeToDismissBox`, end-to-start only, `errorContainer` background with a delete
  icon. TalkBack gets a "Remove" custom accessibility action on each row instead of the swipe.
- **Delete now, undo restores**: the row is deleted from Room at once; the removed `StoredTicker` (code,
  saved quote, `position`, `addedAt`) is kept in the ViewModel while the snackbar is up, and Undo
  re-inserts it unchanged, so it returns to its old place. No "pending delete" state to keep in sync.
  If the process dies while the snackbar is up, the removal stands.
- **One undo at a time**: a newer removal replaces the snackbar; only the latest can be undone.
  The snackbar is `SnackbarDuration.Long`; rotation shows it again.
- **Swipe state is not saveable**: `remember { SwipeToDismissBoxState(...) }`, not
  `rememberSwipeToDismissBoxState()`. LazyColumn restores saveable item state by key, so an undone code
  came back already "dismissed" and was removed again (found on the emulator). Removed rows don't fade out.
- **Storage failure**: the swiped row slides back.

## Consequences
- The store gained `remove` and `restore`; Room does both in transactions.
- Holdings (planned) must not be deleted with the watchlist row: keep them in their own table.
