# 0020: Release signing with a local keystore

Status: accepted (2026-11-18).

## Context
The app is not on the Play Store. To install a production-style build on the phone by hand, the release
APK must be signed with a key that stays the same for every later update. Android refuses to update an
app signed with a different key. The debug build uses the machine's debug key, which is not suitable for
this.

## Decision
- A release keystore is created once with `keytool` and kept at `~/.android/lucrometro-release.jks`,
  outside the repo. The file is 4096-bit RSA, PKCS12, alias `lucrometro`, valid 10,000 days.
- The passwords and the path are kept in `~/.gradle/gradle.properties` under the `lucrometro.release*`
  keys. Nothing secret is in the repo.
- `app/build.gradle.kts` reads those properties. If they are missing, the release build is left unsigned
  and the build does not fail.
- R8 minification is off for now. It can break Room or Compose in ways that need separate testing, so it
  will get its own decision when it is turned on.
- Distribution is manual (the APK is copied to the phone). No Play Store, no Obtainium yet. A later change
  would be recorded in a new decision.

## Consequences
- The keystore and its passwords must be backed up outside this machine (for example in a password
  manager). If they are lost, installed copies can no longer be updated and must be uninstalled, which
  deletes the app's data.
- Every release must keep the same keystore. Bump `versionCode` in `app/build.gradle.kts` for each build.
- Debug and release builds have different signatures. Uninstall one before installing the other.
- Verified on the emulator: `apksigner verify` passes with the release certificate, and the release build
  installs over a clean slate and launches to the watchlist screen.
