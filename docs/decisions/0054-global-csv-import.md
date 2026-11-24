# 0054: Global CSV import from the Portfolio tab

Status: accepted (2026-11-24), extends 0041 and 0042

## Context
The trade CSV (0041) is imported per ticker, from that ticker's trades screen (0042): rows for other tickers are
skipped. The broker's export holds every ticker at once, so filling the log means importing the same file once per
ticker. The owner wants one import for a file with mixed tickers, started from the Portfolio tab, and keeps the
per-ticker import as it is. Designed in Doop on 2026-11-24, page "Global import"; the owner picked the top-bar icon
over an overflow menu and a button at the end of the list, and signed off the five frames (snapshot in
`docs/design/doop/`, `global-import-*`).

## Decision
- **Where**: an import icon button (the same upload-file icon as the trades screen, 48dp, "Import trades from a CSV
  file") in the Portfolio top bar, between the "Updated" stamp and the wallet (`TopAppBar action="import"`). Private
  view only: in public the icon is not there, since the review shows shares and averages.
- **Empty Portfolio** (private): the card text becomes "The portfolio counts the tickers you have trades for. Import
  them all from your broker's CSV, or add them from a ticker's trades." with a filled "Import trades" button at its
  end. In public the card keeps today's text and has no button.
- **Same file**: the app's CSV (0041), the same parser, the same 2 MB limit, the same bad-row errors with the line
  number. Rows are matched to watchlist tickers by the normalized code.
- **Skipped, never an error**: rows for tickers that are not on the watchlist (named, with their count; nothing is
  fetched, no ticker is added) and rows whose id is already saved, or repeated in the file (counted). To import a
  skipped ticker, add it, then import the same file again.
- **All or nothing**: a bad row, any ticker whose shares would go below 0, or any trade dated after today saves
  nothing, for any ticker. The error names the ticker: "SMH.MI would go below 0 shares on 3 Mar 2025: older buys are
  missing from the file." / "The file has a trade for SMH.MI on 3 Dec 2026, which is after today. Check its date."
  followed by "Nothing was saved, for any ticker. Fix the file and import it again." Every ticker's trades are saved
  in one transaction.
- **Nothing to import**: no row for a watchlist ticker is an error ("None of the file's tickers are in your
  watchlist."); rows only already saved give a snackbar ("Nothing new: every trade in the file is already saved.").
- **Review before saving**: a full-screen dialog (Material 3): close, "Import trades" with the file name under it,
  and an "Import" text button in the header. Then a summary ("21 new trades for 5 tickers, from 12 Mar 2024 to
  21 Nov 2026. Shares and averages are after the import."), a divider and:
  - "Will import" (with "After" at the end of the header): one `ImportTickerRow` per ticker with new trades, in
    watchlist order: ticker; buys and sells and their dates (one date, or "first – last"); shares and average after
    the import, the average in the quote's decimals (0045), or "No shares held". When these are a ticker's first
    trades and it has a typed average, a tertiary line "Replaces your typed average, 13.90 EUR" (0040).
  - "Skipped", only when something was: one row per ticker not on the watchlist ("4 trades · not in your
    watchlist"), then "12 trades already saved", then the hint "To import a skipped ticker, add it to the watchlist,
    then import the same file again: trades already saved are skipped."
- **After saving**: back on the Portfolio tab with a snackbar "21 trades imported for 5 tickers" (plurals for both
  counts). The tab recomputes as after any trade change, year-end history included (0052).
- **The per-ticker import** on the trades screen is unchanged (0042).

## Consequences
- **Build notes**: reuse `parseTradeCsv`; turn `checkImport` into a per-ticker check that the global import runs for
  every watchlist ticker in the file, keeping the single-ticker path as it is. The repository needs a save of several
  tickers' trades in one Room transaction, re-checked inside it like `addAll`. `readPickedFile` is then used by two
  screens, so it moves to `ui/common/` (0030). The review is state of the Portfolio screen (a full-screen `Dialog`,
  not a back-stack entry): if the process dies while it is open, the file is picked again.
- **Test**: a unit test for the multi-ticker check (skipped and already-saved counts, a ticker going below 0 named
  and nothing saved, watchlist order), since the emulator can't easily show every case.
- **Strings**: every new text in `values/strings.xml` and `values-it/strings.xml` (0017).
- **Doop**: `TopAppBar` rev 11 adds the `action` prop (none|import, default none), new component `ImportTickerRow`.
  The five frames stay on the page "Global import" until the build merges, then move to "Screens": the empty card into row 9
  "Portfolio" in place of the old empty frame, the others into row 8 "Import".
