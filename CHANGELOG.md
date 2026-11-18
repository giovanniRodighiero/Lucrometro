# Changelog

All notable changes to Lucrometro are documented in this file.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions match the app's `versionName`.

## [0.1] - 2026-11-18

### Added
- Watchlist of ETFs and stocks listed on Borsa Italiana, added by ticker code (for example SMH.MI or ENI.MI).
- Price and day-change pill on each row, with the time of the last update.
- Pull to refresh, and a refresh when the app opens.
- Offline use: the last quotes are saved and shown as stale when they can't be refreshed.
- Swipe to remove a ticker, with an undo snackbar. TalkBack users also get a remove action.
- Italian translation, and a per-app language setting on Android 13 and later.
- App icon and splash screen.

### Known limitations
- No details screen, charts or price history yet.
- Tested on the Android emulator (API 33) only. Not yet tested on a physical phone.
