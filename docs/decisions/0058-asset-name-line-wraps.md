# 0058: The details name line wraps

Status: accepted (2026-11-25), supersedes the "name still ellipsizes" part of 0057 (Details)

## Context
Decision 0057 and the Doop `QuoteHeader` (rev 7) put "ETF · " or "Stock · " before the name on the details screen and
cut the name with "…". The app until then wrapped it. With the prefix and a large font, names are cut: at 200%,
"VanEck Semiconductor UCITS ETF" does not fit. The watchlist and Composition cut names too, so similar fund names could
not be read in full anywhere. The owner saw the wrapping version on the emulator at 130% and chose it (2026-11-25).

## Decision
- On the details screen the name line wraps, on as many lines as it needs, for every ticker.
- "ETF · name" is one text: the next lines start under the word, and the icon stays beside the first line.
- The rest of 0057 stands. Watchlist, year detail and Composition rows still cut the name to one line.

## Consequences
- A long name pushes the price and chart down by a line. At the default font size even SWDA.MI's name fits on one line
  on the emulator (Pixel 7 Pro).
- Doop's `QuoteHeader` and the `details-type-*` snapshot still cut the name: change them when the Doop frames are
  updated after the merge (0057).
