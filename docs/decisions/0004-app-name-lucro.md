# 0004: App name is Lucro

Status: accepted (2026-11-16)

## Context
The project was called "FinanceApp" only as a placeholder folder name. We wanted a short, made-up,
Latin-inspired name for a personal tool that keeps an eye on the user's tickers.

## Decision
The app is called **Lucro**, from Latin *lucrum* (profit, gain). Always written with a capital L.
It is used as the project/folder name, Gradle `rootProject.name` and the launcher label.

## Consequences
- The Kotlin package and applicationId stay `com.financeapp` for now (renaming touches every source
  file and resets the installed app's data on devices). Revisit before any public release.
- "lucro" also means profit in Portuguese; fine for a personal app.
- Not checked against Play Store, domains or trademarks.
