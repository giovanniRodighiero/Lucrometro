# 0053: Total card shows both changes as full-width lines

Status: accepted (2026-11-24), supersedes the total card layout of 0051

## Context
In the M4 build (0052) the total card put "Invested" and "Since you bought" side by side under the divider, as the
frames did; "Invested" already took only its own width. On the owner's phone the "Since you bought" badge still
wrapped to two lines in the common case. The row is simply too narrow: inside the card a 360dp phone has about
296dp, and the amount (about 95dp), the gap (24dp) and the badge "+5,249.20 (+13.94%)" (about 167dp) need about 286dp,
so any narrower screen or larger display size wraps. A 40/60 or 30/70 split would leave the badge even less room.

## Decision
- **Two change lines**: under the total, today's change and the change since you bought are lines of the same kind,
  each a `ChangeBadge` followed by its label ("Today", "Since you bought"), with the full card width. The stale line,
  when shown, comes after them.
- **Invested**: one line below the divider, the label on the left and the amount on the right.
- **Public**: the same two lines in % only; no divider and no Invested line.
- Badges must not wrap at the normal font size on a 360dp phone, even with worst-case values; at larger font sizes
  wrapping stays acceptable (0051).

## Consequences
- Doop: `PortfolioSummary` rev 4; the five frames that use it were re-rendered (their HTML is unchanged), and the
  frame `portfolio-360dp` checks the worst case on a 360dp phone.
- The build changes only the total card in `PortfolioScreen.kt`; nothing else on the tab moves.
