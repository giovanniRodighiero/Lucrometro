# Lucrometro

Native Android app (Kotlin, Jetpack Compose) to monitor 10-15 ETFs and Borsa Italiana (Milan)
stocks, checked a few times per day. No own server: the app calls the data source directly.
Data must be 100% free. All development is done by agents.

Example tickers: SMH.MI (ETF), ENI.MI, ISP.MI, SWDA.MI, VWCE.MI

## Stack
- Kotlin, Jetpack Compose + Material 3, single `:app` module, package, namespace and applicationId `com.lucrometro`
- minSdk 26, targetSdk 36, compileSdk 37 (current Compose libs require 37)
- AGP 9 (Kotlin is built in: do NOT add the `kotlin-android` plugin), Gradle wrapper, version catalog
  in `gradle/libs.versions.toml` (add dependencies there, never inline versions)
- Network: Cronet (`cronet-embedded`) behind the `QuoteSource` interface in `data/`
- Storage: Room 3 (package `androidx.room3`, not `androidx.room`) via KSP, in `data/db/`; schemas exported
  to `app/schemas/` and committed. Bump the version and add a migration on any schema change.
- Packages (decision 0030): `data/` (models, sources, repositories, `db/`), `ui/common/` (shared by 2+ screens),
  `ui/theme/`, `ui/<screen>/`; no screen imports another screen's package. Strings in `res/values/strings.xml`,
  translated in `res/values-it/strings.xml`: add every new string to both (decision 0017)

## Commands
```
./gradlew assembleDebug                      # build
./gradlew test                               # unit tests
./gradlew connectedDebugAndroidTest          # instrumented tests (Room), emulator running; app stays installed
emulator -list-avds                          # pick any API 26+ image (checked so far on a Pixel 7 Pro, API 33)
emulator -avd <name> &                       # boot it
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.lucrometro/.MainActivity   # may fail if run right after install: retry
adb exec-out screencap -p > <scratchpad>/shot.png    # verify UI; keep screenshots out of the repo
scripts/release.sh 0.2 --dry-run             # release checks + signed build, changes nothing
scripts/release.sh 0.2                       # release: needs "## [0.2]" in CHANGELOG.md committed first (decision 0021)
```
Environment: Android SDK with `ANDROID_HOME` set (macOS default: `~/Library/Android/sdk`), `platform-tools` and
`emulator` on `PATH`, JDK 17. Gradle auto-downloads missing SDK platforms. Debug builds need nothing else.
Releases need your own signing keystore in `~/.gradle/gradle.properties` (decision 0020; without it the release
APK is unsigned), the `gh` CLI logged in, and macOS (decision 0021).

## Conventions
- Verify every change: build, install on the emulator, screenshot. Do not report done without it.
- Automated tests (unit, instrumented): write one only when it guards something important that the
  emulator can't easily show (e.g. provider error mapping, retry, cancellation). No coverage targets,
  no TDD, no tests for trivial code. To try something out, use the emulator instead.
- Yahoo is unofficial and answers HTTP 429 even on the crumb call: it happened on 2026-11-18 with curl (probably the
  TLS fingerprint, not frequency; Cronet worked from the same IP). Do not use curl for Yahoo: go through the app's
  Cronet path. Be very careful with live requests, by hand or in code: only what the task needs, one at a time,
  at least 5-10 s apart, never in loops or parallel, reuse saved responses instead of re-fetching, and stop at
  the first 429 (no retries; wait). Slow is fine.
- Data providers stay behind `QuoteSource`; nothing outside `data/` knows about Yahoo or Cronet.
- Manifest needs `INTERNET` and `ACCESS_NETWORK_STATE` (Cronet crashes without the latter).
- Never commit APKs, keys or `local.properties`.
- UI is native Material 3: follow `docs/design/native-look-and-feel.md` (theme tokens, no hard-coded colors).

## Where knowledge lives (keep CLAUDE.md short)
- `CLAUDE.md`: only what an agent needs on every task: stack, commands, conventions.
- `docs/decisions/NNNN-title.md`: one decision per file (context, decision, consequences, status).
  Add one whenever a non-obvious choice is made. Do not edit accepted ones: supersede them.
- `docs/research/`: findings and experiments (what was tested, results, dates, open questions).
- `docs/design/`: design rules for the native UI (look and feel, tokens, Doop-to-Compose mapping).
- `docs/brand/`: app icon artwork (source and store image).
- `docs/roadmap.md`: next steps and untested items.
