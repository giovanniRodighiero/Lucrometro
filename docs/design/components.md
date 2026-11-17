# Doop components to Compose

Mapping from the Doop snapshot (`docs/design/doop/components/`) to composables. Deviations from the frames
are listed under each screen.

| Doop component | Compose |
|---|---|
| `TopAppBar` | Material 3 `TopAppBar`; the stamp is a `bodySmall` text in `actions` |
| `QuoteRow` | `ui/watchlist/QuoteRow.kt` `QuoteRow` (a `Row`, see decision 0014) |
| `ChangeBadge` | `ui/watchlist/QuoteRow.kt` `ChangeBadge` (`Surface` with `CircleShape`) |
| `Fab` | Material 3 `FloatingActionButton` |
| `BottomSheet`, `TextField`, `Button` | `ModalBottomSheet`, `OutlinedTextField`, `Button`/`TextButton` (decision 0013) |
| `StatusBar`, `Keyboard` | Drawn by Android; not ported |

## Watchlist

- Times use the system 12/24h setting ("5:26 PM" on an en-US device; the frame shows "14:32").
- Prices use the device locale's decimal separator; the frame shows a dot.
- Pull-to-refresh, the empty state and the "—" placeholder for a not-yet-fetched row are not in the frames.
