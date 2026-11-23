# 0047: Compact private position card and how realized gains show (M3 design)

Status: accepted (2026-11-23), settles the open points of 0043

## Context
Decision 0043 scoped M3 (gross realized gains, sold-out tickers) and left the look to the Doop frames, drawn after M2
was built. They were drawn on 2026-11-23 on the page "M3 · Realized gains" and the owner signed off every frame. A
worst-case check (a long position with past sells, the 1M chart with the average line and the last buy) showed that
the private card with a full-width "Realized" row pushes the trades footer below a 892dp screen.

## Decision
- **Compact private card.** In the private state the average, shares and value sit in one row of three; "Since avg"
  and "Realized (gross)" sit side by side below; 16dp between rows (was 24). With no sells, "Since avg" is alone on
  the second row. It replaces the 2x2 grid of 0042 in the private state; the other states are unchanged. In the worst
  case the footer stays on screen. Values and badges wrap and are never cut or ellipsized: at a 130% font size they
  take two lines and the card is still shorter than the 2x2 one; the screen scrolls for anything larger.
- **Sold-out card** (0 shares after sells, private): "No shares held", "Realized (gross)" with the ticker's total, and
  the footer to the trades screen. Hidden in public (0043).
- **Gross label**: "Realized (gross)" on the card and "(gross)" in the sold-out summary line. Sell rows and the sheet
  preview show the badge alone, next to "Realized (gross)" in the sheet.
- **Sell rows**: a gain/loss badge with the signed amount and the percentage against the average at the time
  ("+153.56 (+21.68%)") replaces the amount. Buy rows are unchanged.
- **Trades summary line**: unchanged while shares are held ("95 shares · avg 36.72 EUR · 9 trades"); the card already
  shows the realized total. Sold out: "No shares held · realized +631.60 EUR (gross) · 10 trades".
- **Sell sheet preview**: while adding or editing a sell, "Realized (gross)" and its badge show next to the amount as
  you type, from the average at that moment.
- **Percentages**: a sell's is its gain over the cost of the shares sold at the average then; the ticker's is the
  total realized over the total cost of all shares sold.

## Consequences
- Doop: `PositionCard` rev 10 (`sold-out` state, `realized` and `layout` props, `realized` slot), `TradeRow` rev 3
  (`realized` prop and slot). The "Screens" frames keep `layout="regular"`, the M2 card as built, until M3 merges;
  then `compact` becomes the default and those frames are re-exported.
- Supersedes the private 2x2 grid of 0042 for the M3 build.
- To check in the build: TalkBack reads each label with its value; the card at the largest font size (200%); the
  Italian labels in the three columns.
