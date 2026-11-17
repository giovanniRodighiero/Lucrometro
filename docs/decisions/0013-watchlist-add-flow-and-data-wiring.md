# 0013: Watchlist add flow, in-memory repository and data wiring

Status: accepted (2026-11-17)

## Context
The first real screen is the watchlist, built from the Doop frames `watchlist` and `add-ticker`, with
only the add flow done (rows are placeholders; the QuoteRow from 0007 comes later). The PRD requires
validating a pasted code with a live quote before saving and rejecting duplicates. Room (PRD section 5)
is not in yet, and the screen needs a ViewModel that gets a repository without seeing Yahoo or Cronet.

## Decision
- **Wiring**: `data/DataGraph` is a small process-wide object that builds the repository and picks the
  provider (Yahoo via Cronet). ViewModels get it through a `viewModelFactory`. No DI library (Hilt/Koin)
  while there are one or two dependencies; revisit if the graph grows.
- **Repository**: `WatchlistRepository` holds the codes in a `StateFlow`, **in memory only**. The list is
  lost when the process dies until Room replaces the storage behind the same API.
- **Validation**: `add()` normalizes (trim, uppercase), checks the format (`[A-Z0-9.^=-]{1,20}`), rejects
  duplicates, then fetches a quote. It saves only if the quote resolves. Outcomes are typed
  (`AddTickerResult`): empty, bad format, duplicate, not found, no price, check failed. "Not found" and
  "no price" come from typed exceptions in `QuoteSource` (`TickerNotFoundException`, `NoPriceException`),
  so the UI can tell "wrong code" from "network problem". The saved code is the user's normalized input.
- **Cancellation**: the blocking quote call runs in `runInterruptible`; closing the sheet interrupts it and
  Cronet cancels the request, so an abandoned check does not hold the source until it times out.
- **Yahoo session**: a reused cookie/crumb rejected with 401/403 is renewed and the request retried once.
  Symbols and the crumb are URL-encoded.
- **Text field**: uses `TextFieldState` with an `InputTransformation` (drop whitespace, all caps). Rewriting
  the text in `onValueChange` corrupted the keyboard's composing text on the emulator.
- **UI**: Material 3 `ModalBottomSheet` (IME insets handled by the sheet), errors in the field's
  supporting text, Add button shows a spinner while checking. The Doop frames have no error or empty
  states; those are ours.

## Consequences
- The empty state ("No tickers yet") and error messages are not in Doop; add them there if the design
  should own them.
- `HttpClient` interface in front of `CronetHttp` lets `YahooQuoteSource` be unit-tested with a fake;
  `org.json` is a test dependency because the android.jar copy is a stub in local tests.
- Next: Room storage behind `WatchlistRepository`, then the QuoteRow rows and refresh.
