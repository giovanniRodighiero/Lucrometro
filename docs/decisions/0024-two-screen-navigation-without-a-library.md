# 0024: Two-screen navigation without a navigation library

Status: accepted (2026-11-18)

## Context
Tapping a watchlist row opens the details screen (decision 0023). The app has two screens and no deep links.

## Decision
- No navigation library. `MainActivity` keeps the open ticker's code in `rememberSaveable` (null = the
  watchlist) and shows `WatchlistScreen` or `DetailsScreen`. `BackHandler` and the top bar's back arrow set it to null.
- `DetailsViewModel` is keyed by the code and reads the ticker's entry from the same repository as the watchlist.
- The watchlist screen leaves composition while details is open, so on return its refresh-on-open runs again
  (the ViewModel skips it if it refreshed a moment ago).

## Consequences
- Rotation and process death keep the open ticker. No screen transition animation yet.
- If a third screen or deep links appear, move to Navigation 3 (and supersede this decision).
