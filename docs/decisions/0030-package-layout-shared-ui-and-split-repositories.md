# 0030: Package layout v2: shared UI, split repositories, provider-neutral models

Status: accepted (2026-11-18)

Supersedes 0009.

## Context
A review after the details screen (decisions 0022-0029) found the layout of 0009 drifting:
- The details screen imported the change pill and the number and time formats from `ui/watchlist/`, so one screen
  depended on another.
- `WatchlistRepository` had grown a second job: chart history and its session cache.
- `HistoryRange` carried Yahoo's `range` and `interval` strings, so the UI used a type that knew about Yahoo.
- 0009 said "a screen never imports from `data/` except through its ViewModel", but every screen uses the data
  models (`Quote`, `WatchlistEntry`, `HistoryRange`), which is fine and was never going to change.

## Decision
- Root package `com.lucrometro`: `MainActivity` only.
- `data/`: the models in `Models.kt`, the `QuoteSource` contract, its providers (Yahoo via Cronet), the repositories and
  storage (`data/db/`, Room). `DataGraph` is the only place that picks the concrete classes.
  - `WatchlistRepository`: the tracked tickers, their saved quotes, add, remove, refresh.
  - `HistoryRepository`: chart history and its session cache. It depends on the watchlist (one direction only): it asks
    whether a ticker is still tracked, hands a fresh 1D quote to `saveIfNewer`, and drops a ticker's cache when the
    watchlist reports a removal (`onRemoved`).
  - `SourceGate`: the "one request at a time" lock both repositories share.
  - Models are provider-neutral: anything specific to a source (URLs, query values) lives in that source's class.
- `ui/common/`: composables and formatters used by more than one screen (`ChangeBadge`, `QuoteFormats`,
  `TimeFormats`). Something moves here when a second screen needs it, not before.
- `ui/<screen>/`: one folder per screen, with its composables, ViewModel and screen-only models (e.g. `ChartData.kt`).
- `ui/theme/`: unchanged.
- Screens may use data **models**; only ViewModels touch repositories and `DataGraph`. Nothing outside `data/` knows
  about Yahoo, Cronet or Room.
- Unchanged from 0009: tests in `app/src/test/` and `app/src/androidTest/`; text only in `strings.xml`; launcher icon
  in `mipmap-anydpi-v26`, other drawables in `drawable/` with an `ic_` prefix. No `domain/` layer until use cases
  repeat across screens.

## Consequences
- A screen never imports from another screen's package.
- A new data source only needs a new `QuoteSource` and a line in `DataGraph`.
- The details ViewModel takes both repositories.
