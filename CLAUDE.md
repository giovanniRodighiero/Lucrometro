# FinanceApp

Native Android app (Kotlin, Jetpack Compose) to monitor 10-15 ETFs and Borsa Italiana (Milan)
stocks, checked a few times per day. No own server: the app calls the data source directly.
Data must be 100% free. All development is done by agents.

Example tickers: SMH.MI (ETF), ENI.MI, ISP.MI, SWDA.MI, VWCE.MI

## Stack
- Kotlin, Jetpack Compose + Material 3, single `:app` module, package `com.financeapp`
- minSdk 26, targetSdk 36, compileSdk 37 (current Compose libs require 37)
- AGP 9 (Kotlin is built in: do NOT add the `kotlin-android` plugin), Gradle wrapper, version catalog
  in `gradle/libs.versions.toml` (add dependencies there, never inline versions)
- Network: Cronet (`cronet-embedded`) behind the `QuoteSource` interface in `data/`

## Commands
```
./gradlew assembleDebug                      # build
./gradlew test                               # unit tests
emulator -avd Pixel_7_Pro_API_33 &           # boot emulator (arm64, Play Store image)
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.financeapp/.MainActivity   # may fail if run right after install: retry
adb exec-out screencap -p > <scratchpad>/shot.png    # verify UI; keep screenshots out of the repo
```
Environment: `ANDROID_HOME=~/Library/Android/sdk`, JDK 17. Gradle auto-downloads missing SDK
platforms. `PomoTimer` (other project) needs platform 34 and build-tools 34.0.0: do not delete them.

## Conventions
- Verify every change: build, install on the emulator, screenshot. Do not report done without it.
- Data providers stay behind `QuoteSource`; nothing outside `data/` knows about Yahoo or Cronet.
- Manifest needs `INTERNET` and `ACCESS_NETWORK_STATE` (Cronet crashes without the latter).
- Never commit APKs, keys or `local.properties`.
- UI is native Material 3: follow "Look and feel" in `docs/prd.md` (theme tokens, no hard-coded colors).

## Where knowledge lives (keep CLAUDE.md short)
- `CLAUDE.md`: only what an agent needs on every task: stack, commands, conventions.
- `docs/decisions/NNNN-title.md`: one decision per file (context, decision, consequences, status).
  Add one whenever a non-obvious choice is made. Do not edit accepted ones: supersede them.
- `docs/research/`: findings and experiments (what was tested, results, dates, open questions).
- `docs/roadmap.md`: next steps and untested items.

## Current state (2026-11-15)
Spike works: Cronet fetches SMH.MI from Yahoo on the emulator. UI is a single throwaway screen.
See `docs/roadmap.md`.
