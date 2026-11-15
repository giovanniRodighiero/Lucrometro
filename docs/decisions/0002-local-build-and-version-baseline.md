# 0002: Build locally; version baseline

Status: accepted (2026-11-15); revisit if the dev machine changes

## Context
Earlier notes assumed a weak laptop with no Android Studio and builds in GitHub Actions. This Mac
already has Android Studio, the SDK, JDK 17 and an emulator, and a full build takes ~30 s.

## Decision
- Build, install and screenshot locally with `./gradlew` and `adb`; no CI for now.
- Baseline: AGP 9.5.1, Kotlin 2.4.21 (Compose compiler plugin), Gradle 9.9.1 (wrapper),
  Compose BOM 2026.09.00, activity-compose 1.13.0, cronet-embedded 500.1.0.
- compileSdk 37 (required by current Compose libs), targetSdk 36, minSdk 26.
- Single `:app` module until there is a reason to split.
- Emulator: Pixel_7_Pro_API_33 (the only system image kept).

## Consequences
- Fast agent feedback loop (build, run, screenshot).
- Old SDK platforms and unused system images were deleted; platform 34 and build-tools 34.0.0 are
  kept for the PomoTimer project.
- Add GitHub Actions later only if a hosted build or release pipeline is wanted.
