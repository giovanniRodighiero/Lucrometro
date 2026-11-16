# 0003: Native Material 3 look and feel

Status: accepted (2026-11-16)

## Context
The app must feel like a proper native Android app, not a web wrapper. Compose + Material 3 is
already the stack.

## Decision
Follow Material 3 and the Android design guidelines (references and requirements in
`docs/prd.md`, section "Look and feel"). Use standard components and system behaviors (dynamic
color, edge-to-edge, predictive back, light/dark) and custom drawing only for the price chart.

## Consequences
- Screens use theme tokens, never hard-coded colors or text sizes.
- Design reviews check against the PRD checklist and the linked guidelines.
- Less visual freedom in exchange for consistency and less custom code.
