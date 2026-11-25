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
  chosen basis for range changes (decision 0022); checked on 2026-11-18, see below.
- **Details screen data (2026-11-18, Cronet on the emulator, Wed 12:30 CET, market open):** 5 chart calls
  (VWCE.MI 1d/1m, 5y/1wk, max/1mo; SWDA.MI 1y/1d; ENI.MI 1y/1d, `events=div,splits`) plus SMH.MI 1d/1m, one
  at a time, 10 s apart, all HTTP 200. Findings:
  - **1D intraday:** `range=1d&interval=1m` returns one session of 1-minute bars from 09:04 (SMH.MI 188 bars,
    VWCE.MI 190). Many bars are null: SMH.MI 100 of 188, VWCE.MI 76 of 190. The line must skip nulls.
  - **Previous close:** `meta.chartPreviousClose` (equal to `previousClose`) is the previous session's close
    (SMH.MI 76.70 against a price of 80.31). It is in the same 1d response, so no extra request is needed
    for the 1D baseline (decision 0022).
  - **Quote time:** `meta.regularMarketTime` is the last trade, and it equals the last bar's timestamp.
  - **Trading period:** `meta.currentTradingPeriod.regular` is the Milan session (09:00-17:30 CET, epoch
    start/end plus `gmtoffset`); `pre` and `post` are present but not useful. Also `exchangeTimezoneName`
    (Europe/Rome), `priceHint` (decimals) and `validRanges`. It describes today only, so "opens Mon 09:00"
    has to be computed from it (weekends, holidays: not tested).
  - **Delay:** VWCE.MI is liquid (114 bars with a trade in the session). Its last trade was 12:12:59 when it was
    fetched at 12:30:08, so Yahoo's `.MI` data is about 15 minutes behind (SMH.MI: 12:10:07 at 12:29, consistent).
    The details screen must say so (decision 0023 consequences).
  - **`adjclose`:** present in `indicators.adjclose` for ETFs and stocks, daily and weekly. For the accumulating
    ETFs (VWCE.MI, SWDA.MI) it equals `close` everywhere. For ENI.MI (4 dividends in the year) 211 of 254 bars differ:
    2025-11-18 close 16.33, adjclose 15.53. So `close` is raw and `adjclose` adjusted, and decision 0022's range changes
    work as designed. Dividend events are in `events.dividends` (not needed for v1). A distributing ETF was not
    tested, but it should behave like ENI.MI.
  - **Long ranges:** `range=5y&interval=1wk` gives 263 weekly points, 29 KB. `range=max&interval=1mo` for
    VWCE.MI (listed 2020-01-14) came back as 359 **weekly** points (`dataGranularity: 1wk`), not monthly:
    Yahoo returns a finer granularity for short histories, so do not assume the interval asked for. ENI.MI 1y/1d:
    254 points. Sizes are 12-38 KB, so no payload concern.
  - **Last point:** the long ranges end with a bar at the live price and time (VWCE.MI 5y: 166.88 at 12:12:59,
    equal to `regularMarketPrice`), as for the daily ranges.
  - **Method:** curl failed first: the crumb call already returned "Too Many Requests" (a 17-character body, where a
    real crumb is 11) before any chart request, and every later curl call got HTTP 429, still 15 minutes
    later. Cronet from the emulator, on the same Wi-Fi and IP, worked minutes afterwards. Most likely cause:
    Yahoo refuses non-browser TLS fingerprints (as `YahooQuoteSource` assumes), not request frequency. Not proven.
    Use only the app's Cronet path for Yahoo calls, and keep them spaced (CLAUDE.md conventions).
- **5D with intraday bars (2026-11-18, Cronet, Wed 13:57):** `range=5d&interval=30m` for VWCE.MI and SMH.MI, 2 calls
  10 s apart, HTTP 200, 8 KB each. Both returned 79 bars with no null closes: 5 sessions (Thu 12 to Wed 18) of 17 half-hour
  bars (09:00-17:30) and 11 for today. So 5D can be a real line instead of the 5 daily points. There is **no `adjclose`**
  on intraday bars, so a dividend inside the 5 days would show as a drop (acceptable: it is rare and small, but the 5D
  change is then on plain closes). `meta.chartPreviousClose` is the close before the first bar of the range (VWCE.MI 170.92),
  not the previous session's. The overnight and weekend gaps are large in time (a time-proportional axis would leave
  holes), so the 5D chart should place bars by index.
- **Quote vs 1D chart (2026-11-18, Cronet, Wed 16:04):** SMH.MI `range=1d&interval=1d` (the refresh's quote) and
  `range=1d&interval=5m` (the 1D chart), 15 s apart: the same `meta` (price 77.61, `regularMarketTime` 15:48:49, day
  high/low and volume), and the last 5m bar equals it. The interval does not change the quote. Prices that differ between
  the list and the details header come from a newer delayed trade arriving between two requests (decision 0029).
  The `interval=5m` meta also has `previousClose` and `tradingPeriods`; the `interval=1d` one does not.
- **US pre-market spike (2026-11-25, Cronet on the emulator, Wed ~11:37 CET / 05:37 New York):** works. Cookie, crumb,
  then 2 calls for SMH (NasdaqGM), 8 s apart, no 429.
  - **Chart (the endpoint the app already uses):** `v8/finance/chart/SMH?range=1d&interval=5m&includePrePost=true`.
    `meta` has `hasPrePostMarketData: true`, `fulldayPrice` 408.95, `fulldayChange` -17.80 and `fulldayChangePercent`
    -4.17 (against `regularMarketPrice` 426.75, yesterday's close), plus `currentTradingPeriod.pre` (04:00-09:30
    New York). 20 bars from 04:00; the last one (05:35:20) closes at 408.95. All pre-market bar volumes are 0, and the
    first bar's high (420.46) looks like a stray print.
  - **Quote:** `v7/finance/quote?symbols=SMH` has `marketState: "PRE"`, `preMarketPrice` 408.95, `preMarketChange`,
    `preMarketChangePercent` and `preMarketTime`, matching the chart. It reports `exchangeDataDelayedBy: 15`.
  - **Not tested:** `fulldayPrice` during regular and post-market hours or without `includePrePost`; `includePrePost`
    with `interval=1d` (the app's refresh); weekends and holidays; a thinly traded US stock; the real delay (`preMarketTime` was under 2.5 minutes old, despite the reported 15; one check only); `.MI`.
- **Which request carries the pre-market price (2026-11-25, Wed 12:36 CET / 06:36 New York):** the build uses
  `v7/finance/quote` (decision 0055).
  - The unchanged 1D call (`range=1d&interval=5m`, one SMH request) keeps the 24 Nov session (79 bars, 09:30-16:00;
    `tradingPeriods` is that day) and already has `fulldayPrice` 408.30, `hasPrePostMarketData: true` and today's
    `currentTradingPeriod.pre`. It has **no time** for that price and no market state; the design shows the time.
  - With `includePrePost=true` (the spike above) the same range returned only today's pre-market bars, so the 1D chart
    would lose the last session. It cannot go on the chart call.
  - So: one extra quote request after the 1D chart, only when `hasPrePostMarketData` is true and the clock is inside
    `currentTradingPeriod.pre`.
  - Checked in the app (NBIS, 06:38 New York): the price was stamped 06:38, so no visible 15-minute delay.
  - **Milan (13:25 CET, one SMH.MI 1D chart call):** `hasPrePostMarketData: false`, yet `currentTradingPeriod.pre` is
    there (08:00-09:00 CET). `fulldayPrice` is present too (equal to `regularMarketPrice`, market open). So only the
    flag keeps `.MI` from the extra request.
- **Classification data (2026-11-25, Cronet on the emulator):** cookie, crumb and 9 requests, 10 s apart, all HTTP 200.
  - **`v7/finance/quote`** (one call, ENI, ISP, RACE, SMH, SWDA, VWCE, AGGH, IPRP, SGLD, all `.MI`): `quoteType` is
    `EQUITY` for the 3 stocks and `ETF` for the other 6, including the bond ETF (AGGH) and the gold ETC (SGLD). No
    sector, industry or category field.
  - **`v10/finance/quoteSummary/{symbol}?modules=quoteType,assetProfile,fundProfile,topHoldings`** (one call per symbol:
    ENI, SMH, SWDA, AGGH, IPRP, SGLD):
    - Stock (ENI): `assetProfile.sector` "Energy", `industry` "Oil & Gas Integrated" (also `sectorKey`, `industryKey`), `country`.
    - ETFs: `fundProfile.categoryName` is **null in all 5**; `legalType` is "Exchange Traded Fund", also for the ETC.
    - `topHoldings` asset mix: SMH, SWDA, IPRP about 99-100% `stockPosition`; AGGH 99% `bondPosition`; SGLD 100% `otherPosition`.
    - `topHoldings.sectorWeightings` (11 sectors): SMH technology 100%, IPRP realestate 100%, SWDA spread (largest:
      technology 30.6%). Empty for AGGH and SGLD. No "semiconductors" or "world" label, and no geography field.
    - `topHoldings.holdings` has the top 10 for the 3 equity ETFs. AGGH also has `bondRatings` and duration/maturity.
      Some numbers look wrong (equity P/E 0.03-0.12, AGGH net assets 0): do not rely on them without checking.
  - **`v1/finance/search`:** `q=ENI.MI` gives `quoteType`, `sector` and `industry`. `q=IT0005436693` (a BTP ISIN) gives
    no result.
  - **Not tested:** sector for other stocks, VWCE's summary, distributing/other-issuer coverage beyond these, single bonds
    beyond that one ISIN search, how often the data changes.
- **Type in the chart response (2026-11-25, app build on the emulator):** `v8/finance/chart` `meta.instrumentType` gives
  the type, so the app needs no extra request (decision 0057). Requests came from normal app use during testing
  (refreshes after several restarts, one add, a details chart, history for the test trades); not counted. Seen: ETF for SMH.MI and SWDA.MI, stock (`EQUITY`) for ENI.MI, NBIS and 0700.HK. The app maps only `ETF` and
  `EQUITY`. Raw values were not logged, and other kinds (index, fund, currency, ETC) were not seen in the chart.
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
- Exchange holidays and weekends for the details screen: `currentTradingPeriod` was only checked on an open
  weekday. Check one request on a weekend (what the 1D range returns, the period dates, the previous close).
- Whether the ~15 minute `.MI` delay is constant (checked once, on 2026-11-18).
- 2026-11-20, SMH.MI 1D through the app: `meta.fullExchangeName` is "Milan"; no delay in `meta`
  (`exchangeDataDelayedBy` absent or 0), see decision 0037.
- Long-term stability: bursts are fine, but does the session/crumb expire, and does Yahoo throttle
  at a few checks per day over weeks? (crumb refresh on HTTP 401/429 is implemented only as 'reset on any non-200')
- Real phone: 0.3 works on the owner's phone (2026-11-20). Whether that was Wi-Fi or mobile data was not recorded.
- Yahoo WebSocket for `.MI` symbols.
