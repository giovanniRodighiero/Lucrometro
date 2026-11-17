# 0017: English and Italian copy, per-app language

Status: accepted (2026-11-17)

## Context
All user-visible copy was already in `res/values/strings.xml` (decision 0009), with numbers and times
formatted in the device locale. The owner wants multi-language support, starting with Italian.

## Decision
- English stays the default (`res/values/`), declared in `res/resources.properties`
  (`unqualifiedResLocale=en`). Italian lives in `res/values-it/strings.xml`.
- Android picks the language from the phone; on Android 13+ the user can also choose it per app
  (Settings > Apps > Lucrometro > Language). The list comes from AGP's `generateLocaleConfig`, built from
  the `values-*` folders: no hand-written `locales_config.xml` (the build fails if both exist).
- Not translatable: `app_name` and the "—" placeholder (`translatable="false"`). Ticker codes and company
  names from Yahoo are data, never translated.
- Italian copy uses "titolo" for ticker, informal "tu". Stale note "Ultimo dato: <time>" instead of a literal
  "Non aggiornato dalle", which reads badly when the time includes a date. Percent stays "+1,24%": Italian
  (CLDR) has no space before %.
- Lint's MissingTranslation check (an error by default) flags any new English string without an Italian one.

## Consequences
- Every new string goes into both files. Counts must use `<plurals>`.
- Below Android 13 the app follows the phone language only; an in-app picker would need AppCompat's
  `setApplicationLocales` and is not planned.
