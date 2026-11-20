# Doop components to Compose

Mapping from the Doop snapshot (`docs/design/doop/components/`) to composables. Deviations from the frames
are listed under each screen.

| Doop component | Compose |
|---|---|
| `TopAppBar` | Material 3 `TopAppBar`; the stamp is a `bodySmall` text in `actions` |
| `QuoteRow` | `ui/watchlist/QuoteRow.kt` `QuoteRow` (a `Row`, see decision 0014) |
| `ChangeBadge` | `ui/common/ChangeBadge.kt` `ChangeBadge` (`Surface` with `CircleShape`) |
| `DetailsTopBar` | `ui/details/DetailsScreen.kt`: Material 3 `TopAppBar`, back arrow and ticker; in `actions`, `MarketMarker` (`MarketStatus.kt`: 8dp dot and one word, opens `MarketSheet.kt`) and the sync button (`IconButton`, `FilledTonalIconButton` when on; decisions 0035, 0037). The status text of decisions 0027 and 0032 is in the sheet (`frames/market-status-cases`) |
| `QuoteHeader` | `ui/details/QuoteHeader.kt` (`Column` with a `FlowRow` for pill and stamp; decision 0023) |
| `ChartReadout` | `ui/details/ChartReadout.kt` (summary mode, and scrub mode via `scrubbing`; the label is `scrubLabel` in the same file, decision 0028) |
| `PriceChart` | `ui/details/PriceChart.kt`: a `Canvas`, see decision 0025; the average line, edge marker and last-buy diamond, decisions 0033, 0034, 0036 |
| `PositionCard` | `ui/details/PositionCard.kt` (`Column` on `surfaceContainerLow`; decisions 0033, 0036); the add/edit sheet is `ui/details/PositionSheet.kt` (`ModalBottomSheet`, stock `DatePickerDialog`) |
| `RangeTab` | `ui/details/RangeBar.kt`: a `Row` of `selectable` cells in a `selectableGroup`, `Role.RadioButton` (decision 0026) |
| `Fab` | Material 3 `FloatingActionButton` |
| `BottomSheet`, `TextField`, `Button` | `ModalBottomSheet`, `OutlinedTextField`, `Button`/`TextButton` (decision 0013) |
| `StatusBar`, `Keyboard` | Drawn by Android; not ported |

## Watchlist

- Times use the system 12/24h setting ("5:26 PM" on an en-US device; the frame shows "14:32").
- Prices use the device locale's decimal separator; the frame shows a dot.
- Swipe-to-remove (red `errorContainer` background, delete icon) and the undo snackbar are not in the frames
  (decision 0018).
- Pull-to-refresh, the empty state and the "—" placeholder for a not-yet-fetched row are not in the frames.

## Details

- The change pill is the watchlist `ChangeBadge` with an `absolute` value ("+0.86 (+2.14%)"). Abs uses the quote's decimals.
- The stamp is the quote's own time (`regularMarketTime`) in the exchange's clock (decision 0031) and the system 12/24h setting; "Today, 14:32", otherwise the session day.
- Back is the system back and the top bar arrow (decision 0024). The frames' status bar and gesture bar are not ported.
- The chart loading state (a progress indicator) is not in the frames.
- Not in the frames: the empty 1D card ("No trades yet"), 5D (weekday labels, bars by index), 8dp dots for the market status are drawn as in the top bar frame.
