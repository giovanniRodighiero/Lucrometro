# 0007: Watchlist uses minimal rows with tonal change pills

Status: accepted (2026-11-17)

## Context
Four watchlist proposals were explored on the Doop canvas "Lucrometro": A (plain list, coloured
change text), B (grouped cards, leading direction icon, summary card, extended FAB), C (A with the
day change in a filled pill) and D (C with each row on a card background).

## Decision
Use option C: the plain Material 3 list from A, one row per ticker (code, name, price with currency,
day change), with the day change shown as a filled tonal pill: arrow plus signed value on a
gain/loss/neutral container colour. Small regular FAB for add. No summary card, no leading icon,
no card backgrounds.

## Consequences
- Theme needs the semantic tokens `gain`, `loss`, `gainContainer`, `onGainContainer`,
  `lossContainer`, `onLossContainer` in light, dark and dynamic color.
- Fits ~9 rows per screen on a 412dp-wide phone; full ETF names usually fit.
- The change pill is the reusable "change" element for the details screen too.
