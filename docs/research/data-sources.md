# Data sources research (2026-11-15, a Sunday: market closed)

## Yahoo Finance (unofficial, same source yfinance uses)
- Works for Milan: yfinance 1.8.0 returned SMH.MI (81.75 EUR, exchange MIL, daily history).
- Flow: cookie from `fc.yahoo.com` (sets `A3`, the 404 is expected) -> crumb from
  `/v1/test/getcrumb` -> `/v8/finance/chart/{symbol}?range=1d&interval=1d&crumb=...`.
  Also `/v7/finance/quote`.
- Plain clients are rejected: stdlib `urllib` and `curl` got HTTP 429 on every call, even the
  crumb request, while yfinance (uses `curl_cffi` with `impersonate="chrome"`) succeeded on the
  same network at the same time. So the block is on the client TLS fingerprint, not the IP.
- **Cronet result (emulator on this Mac's network):** the same flow works. SMH.MI 81.75 EUR,
  quote time 2026-11-13 17:36:05 (Friday close), 845 ms for the whole flow. Cookies must be
  handled manually (we read `Set-Cookie` and resend `Cookie`).
- **Multi-ticker test (2026-11-15, emulator, no delay between calls):** 15 tickers in a row
  (SMH, ENI, ISP, SWDA, VWCE, UCG, ENEL, RACE, G, LDO, MB, PRY, STMMI, CSSPX, EIMI, all `.MI`):
  15/15 OK, EUR prices, ~70-150 ms per ticker, ~2.3 s for the first round (includes cookie+crumb).
  Then 10 more rounds on the same session (150 more calls, ~1.5 s per round at most):
  all OK, no 429, no re-authentication needed. One cookie+crumb session was enough.
  Tickers that worked beyond the examples: see list above (all returned valid quotes).
  This is a burst test of ~165 calls in a few minutes, not a day-long test.
- **History (2026-11-15):** `/v8/finance/chart/{symbol}?range=5d|1mo|3mo&interval=1d&crumb=...`
  returns daily OHLCV for SMH.MI: 5d = 5 bars (2026-11-09..13), 1mo = 24 bars (2026-10-13..11-13),
  3mo = 67 bars (2026-08-13..11-13). Same session as quotes, 70-600 ms. Last close (81.75) equals the
  quote price and the shorter ranges match the tail of the longer ones. `range=1mo` is a calendar
  month (~22-24 trading days), not exactly 30 days; use `period1`/`period2` (epoch) if exact
  windows are needed. Bars with a null close are dropped. Not yet checked: dividend/split
  adjusted closes (`adjclose`), gaps for illiquid ETFs, longer ranges. Adjusted closes are the
  chosen basis for range changes (decision 0013) and still need to be checked.
- Unofficial API: it can change or block without notice, hence the `QuoteSource` interface.

## Licensed APIs checked: none give free Milan quotes
- Twelve Data: free = 8 calls/min, 800/day, but Milan (XMIL) needs the Pro plan (~$229/month),
  confirmed via `GET /exchanges?code=XMIL&show_plan=true`.
- Finnhub: quote endpoint is US-only; international real-time is Enterprise-only (per its docs).
- EODHD: free = 20 calls/day, end-of-day only, past year, Milan coverage not confirmed; paid from
  $19.99/month (Milan quotes on paid plans not verified).
- Not checked: Alpha Vantage, Stooq.
- Why: exchanges license their data; free tiers focus on cheaper US data.

## Open questions
- Intraday delay for `.MI` (weekday 09:00-17:30 CET: compare quote time to the clock).
- Long-term stability: bursts are fine, but does the session/crumb expire, and does Yahoo throttle
  at a few checks per day over weeks? (crumb refresh on HTTP 401/429 is implemented only as 'reset on any non-200')
- Behaviour on a real phone and mobile networks.
- Yahoo WebSocket for `.MI` symbols.
