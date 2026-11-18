# Doop components to Compose

Mapping from the Doop snapshot (`docs/design/doop/components/`) to composables. Deviations from the frames
are listed under each screen.

| Doop component | Compose |
|---|---|
| `TopAppBar` | Material 3 `TopAppBar`; the stamp is a `bodySmall` text in `actions` |
| `QuoteRow` | `ui/watchlist/QuoteRow.kt` `QuoteRow` (a `Row`, see decision 0014) |
| `ChangeBadge` | `ui/watchlist/QuoteRow.kt` `ChangeBadge` (`Surface` with `CircleShape`) |
| `DetailsTopBar` | `ui/details/DetailsScreen.kt`: Material 3 `TopAppBar`, back arrow and ticker; market status not built yet |
| `QuoteHeader` | `ui/details/QuoteHeader.kt` (`Column` with a `FlowRow` for pill and stamp; decision 0023) |
| `ChartReadout` | `ui/details/ChartReadout.kt` (summary mode; scrub mode comes with the scrub) |
| `PriceChart` | `ui/details/PriceChart.kt`: a `Canvas`, see decision 0025 |
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
- The stamp is the quote's own time (`regularMarketTime`) in the system 12/24h setting; "Today, 14:32", otherwise the session day.
- Back is the system back and the top bar arrow (decision 0024). The frames' status bar and gesture bar are not ported.
- The chart loading state (a progress indicator) is not in the frames.
