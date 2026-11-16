# 0006: Application ID and package are com.lucrometro

Status: accepted (2026-11-16)
Amends: 0005 (its consequence "package and applicationId stay `com.financeapp`")

## Context
`com.financeapp` was a placeholder from before the app had a name. The application ID is the app's
identity on the device and on the Play Store, and cannot change after publishing.

## Decision
`applicationId`, Gradle `namespace` and the Kotlin package are all `com.lucrometro`
(sources moved to `app/src/main/java/com/lucrometro`).

## Consequences
- The old debug build (`com.financeapp`) is a different app to Android: uninstall it from the emulator
  (`adb uninstall com.financeapp`); its local data is not carried over.
- The launch component is `com.lucrometro/.MainActivity`.
