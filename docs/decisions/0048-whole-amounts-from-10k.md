# 0048: Change amounts without decimals from 10,000

Status: accepted (2026-11-23), refines 0047

## Context
On the M3 build, a position with 5-figure gains ("+61,310.00 (+306.55%)" for "Since avg", "+15,000.00 (+150.00%)"
for "Realized (gross)") wrapped both badges of the compact private card onto two lines at the default font size.
0047 allows badges to wrap, but the owner wants them on one line where possible; the cents add nothing at that size.

## Decision
A change amount (the signed amount in a gain/loss badge) of 10,000 or more in absolute value is shown without
decimals: "+61,310 (+306.55%)". Below 10,000 it keeps the quote's decimals. Percentages keep 2 decimals. Totals that
are not changes (the position's value, the sold-out summary line, trade prices and amounts) are unchanged.

## Consequences
- It applies wherever a badge shows an amount: the position card, sell rows, the sell sheet preview, and the quote
  header (where a per-share change of 10,000 is not expected).
- Rounding to whole units can show a different last digit than the sum of the cents; the gain direction follows the
  rounded amount, as before.
