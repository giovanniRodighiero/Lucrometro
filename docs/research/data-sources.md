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
- Repeated and multi-ticker fetch stability; does Yahoo throttle or block after a while?
- Behaviour on a real phone and mobile networks.
- Yahoo WebSocket for `.MI` symbols.
