# 0016: Review fixes: session staleness, list position, one source lock

Status: accepted (2026-11-17). Supersedes the "not persisted" and ordering parts of 0015.

## Context
A medium code review of 0014-0015 found: an add waiting behind a refresh could not be cancelled; saved
quotes looked current after a restart; storage errors in refresh would crash the app; the auto-refresh
throttle ignored pull-to-refresh; the list order relied on a wall clock; the quote-to-column mapping was
written twice; text around numbers was built in code.

## Decision
- **One request at a time** through a coroutine `Mutex` in `WatchlistRepository`, shared by add and refresh.
  `YahooQuoteSource` is `@Synchronized`, and a thread waiting for a Java monitor ignores interrupts, so the
  wait must happen on the cancellable coroutine lock first. Guarded by a unit test (fails without the lock).
- **Staleness**: a quote is current only if fetched in this app session (process). After a restart every
  saved quote shows "Stale since <fetch time>" until this session's refresh replaces it. The "Updated"
  stamp is the newest saved fetch time, so it survives restarts too.
- **Storage errors** are caught in the repository: in refresh the ticker is marked failed (stale); in add the
  sheet shows "Couldn't save X". Cancellation still propagates.
- **Throttle**: the repository records when any refresh ended; the automatic refresh is skipped within
  60 s of it.
- **Schema v2**: explicit `position` column (1..n), assigned in a transaction on insert. Not `rowid`
  (VACUUM may renumber it) and not `addedAt` (the clock can go backwards). Migration 1→2 numbers existing
  rows by their v1 order; verified on the emulator with 9 stored tickers.
- **One mapping**: quote columns are an `@Embedded QuoteColumns`, written by insert and by a partial
  `@Update(entity = TickerEntity::class)`, so a new field can't be saved on add and dropped on refresh.
- **Formatting**: `QuoteFormats` (one per locale, remembered for the list) holds the number formats; the
  currency position, sign and percent come from strings.xml.

## Consequences
- Every launch briefly shows stale marks until the refresh lands: honest, per the PRD.
- The "Couldn't fetch" and pending row states stay, documented for future import/holdings flows.
- No instrumented migration test yet; add one (room3-testing `MigrationTestHelper`) with the holdings schema.
