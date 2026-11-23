# 0044: Navigation 3 for three screens

Status: accepted (2026-11-23), supersedes 0024

## Context
Decision 0024 kept the open ticker's code as the whole navigation state and said to move to Navigation 3 when a third
screen appeared. M2 adds the per-ticker trades screen (0042).

## Decision
- `NavDisplay` from `androidx.navigation3:navigation3-ui` (stable 1.2.x). The back stack is a list of `Screen` keys
  (`Watchlist`, `Details(code)`, `Trades(code)`) in `MainActivity`, which owns it.
- The back stack is saved with a small `listSaver` of plain strings, not `rememberNavBackStack`: that one needs the
  kotlinx-serialization plugin for three keys.
- No ViewModel decorator: ViewModels stay scoped to the activity and keyed by ticker, as before, so the details
  screen keeps its reopen behaviour (0029).
- System back and the top bars' back arrows pop the last key. The default `NavDisplay` transitions are used.

## Consequences
- Rotation and process death keep the whole stack. Screens animate in and out.
- Deep links, if they ever come, are keys pushed on the stack.
