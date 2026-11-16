# 0005: App name is Lucrometro

Status: accepted (2026-11-16)
Supersedes: 0004

## Context
"Lucro" (0004) was a bare Latin/Portuguese word for "profit". We wanted a name that keeps the
*lucrum* root but says what the app does: a gauge you glance at a few times a day.

## Decision
The app is called **Lucrometro**: *lucro* (profit, from Latin *lucrum*) + *metro* (meter).
Always written with a capital L. It is used as the project/folder name, the GitHub repository name,
Gradle `rootProject.name` and the launcher label.

## Consequences
- The Kotlin package and applicationId stay `com.financeapp` (renaming touches every source file
  and resets the installed app's data on devices). Revisit before any public release.
- The label is 10 characters; check it is not truncated under the launcher icon on the emulator.
- Not checked against Play Store, domains or trademarks.
- Old links to the GitHub repo `Lucro` redirect to the new name.
