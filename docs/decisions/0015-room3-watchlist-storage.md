# 0015: Watchlist stored with Room 3

Status: accepted (2026-11-17). Supersedes the "in memory" parts of 0013 and 0014.

## Context
The watchlist and last quotes were in memory and lost whenever the process died. PRD section 5 asks Room
to store the ticker codes (order of addition) and the last successful quote per ticker, so the list
survives restarts and shows stale quotes offline.

## Decision
- **Room 3.0.3** (`androidx.room3`, the current stable line; Room 2 is the old `androidx.room` package),
  with KSP 2.3.14 (works with AGP 9's built-in Kotlin) and the Room Gradle plugin exporting schemas to
  `app/schemas/` (committed, the base for future migrations).
- **Driver**: `AndroidSQLiteDriver` (`androidx.sqlite:sqlite-framework`), the platform SQLite, rather than
  `BundledSQLiteDriver`, which ships a native SQLite in the APK. One small table does not need it.
- **Schema**: one table `watchlist`: `code` (primary key), `addedAt` (list order) and nullable quote
  columns (price, currency, exchange, name, previous close, decimals, market time, fetched at). One table
  means removing a ticker also removes its cached quote, as the PRD requires.
- **Boundary**: `WatchlistRepository` talks to a `WatchlistStore` interface; `data/db/RoomWatchlistStore`
  implements it. Unit tests use an in-memory fake, so they stay on the JVM without Robolectric.
- **Not persisted**: which tickers failed their latest fetch, and the "Updated" time. After a restart a row
  shows its saved quote (with the refresh indicator running) until the first refresh marks it fresh or stale.
- **Loading**: the ViewModel exposes `null` until the first read from the database, so the empty state does
  not flash on launch.

## Consequences
- Verified on the emulator: list survives force-stop; reopened offline it shows every saved quote marked
  "Stale since" with the fetch time (PRD success criterion).
- The "Couldn't fetch" row still can't happen: every stored ticker has a quote, since add saves one.
- Next: remove with undo (PRD 3) needs a `delete` on the store.
