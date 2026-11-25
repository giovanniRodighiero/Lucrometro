package com.lucrometro.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Unofficial Yahoo Finance source. Flow: cookie (fc.yahoo.com) -> crumb -> chart endpoint.
 * Yahoo rejects non-browser TLS fingerprints, hence Cronet.
 */
class YahooQuoteSource(private val http: HttpClient) : QuoteSource {
    private var cookie: String? = null
    private var crumb: String? = null

    @Synchronized
    override fun quote(symbol: String): Result<Quote> = runCatching {
        // A known symbol can still come back without a price (suspended, delisted, no trades yet).
        val meta = chart(symbol, "range=1d&interval=1d").optJSONArray("result")?.optJSONObject(0)?.optJSONObject("meta")
            ?: throw NoPriceException(symbol)
        quoteFrom(symbol, meta) ?: throw NoPriceException(symbol)
    }

    /** The quote in a chart response's `meta`, or null when it has no price. For range=1d the previous close is the session before. */
    private fun quoteFrom(symbol: String, meta: JSONObject): Quote? {
        if (!meta.has("regularMarketPrice") || meta.isNull("regularMarketPrice") || !meta.has("regularMarketTime")) return null
        return Quote(
            symbol = meta.optString("symbol").ifEmpty { symbol },
            price = meta.getDouble("regularMarketPrice"),
            currency = meta.optString("currency").ifEmpty { null },
            exchange = meta.optString("exchangeName").ifEmpty { null },
            marketTimeEpoch = meta.getLong("regularMarketTime"),
            name = meta.optString("longName").ifEmpty { meta.optString("shortName") }.ifEmpty { null },
            previousClose = meta.optDouble("chartPreviousClose").takeIf { !it.isNaN() },
            priceDecimals = meta.optInt("priceHint", -1).takeIf { it in 0..6 },
            exchangeZone = meta.optString("exchangeTimezoneName").ifEmpty { null },
        )
    }

    @Synchronized
    override fun preMarket(symbol: String): Result<PreMarket?> = runCatching {
        // The chart has a pre-market price but not its time, and with includePrePost it loses the last session's bars.
        // No session renewal on a refusal, unlike the chart: an optional price must not multiply requests or drop the
        // session the chart uses. The chart request just before this one has renewed an expired session.
        val res = get("v7/finance/quote?symbols=${encode(symbol)}")
        if (res.status == 429) throw RateLimitedException()
        if (res.status != 200) error("quote HTTP ${res.status}: ${res.body.take(120)}")
        val q = JSONObject(res.body).getJSONObject("quoteResponse").optJSONArray("result")?.optJSONObject(0)
        if (q == null || q.optString("marketState") != "PRE") return@runCatching null
        val price = q.optDouble("preMarketPrice")
        val percent = q.optDouble("preMarketChangePercent")
        val time = q.optLong("preMarketTime")
        // A time before the last regular trade is an earlier day's pre-market, still in the response.
        if (price.isNaN() || percent.isNaN() || time <= q.optLong("regularMarketTime")) null else PreMarket(price, percent, time)
    }

    @Synchronized
    override fun history(symbol: String, range: HistoryRange): Result<History> = runCatching {
        val (yahooRange, interval) = range.yahoo
        parseHistory(symbol, range, chart(symbol, "range=$yahooRange&interval=$interval").getJSONArray("result").getJSONObject(0))
    }

    @Synchronized
    override fun dailyHistory(symbol: String, fromEpochSeconds: Long): Result<History> = runCatching {
        // period2 in the future is fine: Yahoo ends at the last bar it has.
        val period2 = System.currentTimeMillis() / 1000 + 86_400
        val result = chart(symbol, "period1=$fromEpochSeconds&period2=$period2&interval=1d").getJSONArray("result").getJSONObject(0)
        // Yahoo may answer a coarser or finer bar size than asked (VWCE.MI max/1mo came back weekly): a weekly bar can
        // run past 31 Dec, so only daily bars give the year-end close (decision 0052).
        val granularity = result.optJSONObject("meta")?.optString("dataGranularity").orEmpty()
        if (granularity != "1d") error("daily history came back as '$granularity'")
        parseHistory(symbol, null, result)
    }

    /** The bars of a chart `result`; [range] is null for a period request (no quote taken from it). */
    private fun parseHistory(symbol: String, range: HistoryRange?, result: JSONObject): History {
        // No bars at all (1D before the first trade of the day): no "timestamp" key, an empty history.
        val ts = result.optJSONArray("timestamp") ?: JSONArray()
        val q = result.getJSONObject("indicators").optJSONArray("quote")?.optJSONObject(0) ?: JSONObject()
        val closes = q.optJSONArray("close")
        val adj = result.getJSONObject("indicators").optJSONArray("adjclose")?.optJSONObject(0)?.optJSONArray("adjclose")
        val candles = (0 until ts.length()).mapNotNull { i ->
            val close = closes?.takeIf { !it.isNull(i) }?.getDouble(i) ?: return@mapNotNull null // halted/empty bar
            Candle(
                epochSeconds = ts.getLong(i),
                close = close,
                adjClose = adj?.takeIf { !it.isNull(i) }?.getDouble(i),
            )
        }
        val meta = result.optJSONObject("meta") ?: JSONObject()
        val current = meta.optJSONObject("currentTradingPeriod")
        return History(
            candles = candles,
            previousClose = meta.optDouble("chartPreviousClose").takeIf { !it.isNaN() },
            sessions = regularSessions(meta.opt("tradingPeriods")),
            currentSession = current?.optJSONObject("regular")?.let(::period),
            // Milan has a "pre" period too, with the flag false (docs/research/data-sources.md).
            preSession = current?.optJSONObject("pre")?.takeIf { meta.optBoolean("hasPrePostMarketData") }?.let(::period),
            exchangeZone = meta.optString("exchangeTimezoneName").ifEmpty { null },
            // Best effort: a malformed quote field must not fail a chart whose bars are fine.
            quote = if (range == HistoryRange.DAY_1) runCatching { quoteFrom(symbol, meta) }.getOrNull() else null,
            exchangeName = meta.optString("fullExchangeName").ifEmpty { null },
            delayMinutes = meta.optInt("exchangeDataDelayedBy", -1).takeIf { it > 0 },
            lastTradeEpoch = meta.optLong("regularMarketTime").takeIf { it > 0 },
        )
    }

    private fun period(o: JSONObject): TradingPeriod? =
        if (o.has("start") && o.has("end")) TradingPeriod(o.getLong("start"), o.getLong("end")) else null

    /** `tradingPeriods` is a list of lists of periods (one per day), or an object with a "regular" list of them. */
    private fun regularSessions(raw: Any?): List<TradingPeriod> {
        val days = when (raw) {
            is JSONArray -> raw
            is JSONObject -> raw.optJSONArray("regular")
            else -> null
        } ?: return emptyList()
        return (0 until days.length()).flatMap { i ->
            when (val day = days.opt(i)) {
                is JSONArray -> (0 until day.length()).mapNotNull { j -> day.optJSONObject(j)?.let(::period) }
                is JSONObject -> listOfNotNull(period(day))
                else -> emptyList()
            }
        }
    }

    /**
     * Returns the `chart` object of the response; throws on HTTP or API errors. A reused session that Yahoo rejects
     * (expired cookie or crumb: 401/403) is renewed and the request retried once. [query]: the bars asked for.
     */
    private fun chart(symbol: String, query: String): JSONObject {
        val reused = cookie != null && crumb != null
        // Codes may hold ^ or = (indices, currencies).
        val path = "v8/finance/chart/${encode(symbol)}?$query"
        var res = get(path)
        if (reused && res.status in AUTH_FAILURES) {
            clearSession()
            res = get(path)
        }
        return parseChart(symbol, res)
    }

    /** [pathAndQuery] has its query already; the crumb is appended. */
    private fun get(pathAndQuery: String): HttpResult {
        ensureSession()
        // The crumb may hold any character: encode it.
        val url = "https://query1.finance.yahoo.com/$pathAndQuery&crumb=${encode(crumb.orEmpty())}"
        return http.get(url, mapOf("Cookie" to cookie.orEmpty(), "Accept" to "application/json"))
    }

    private fun parseChart(symbol: String, res: HttpResult): JSONObject {
        if (res.status == 404 && isNotFound(res.body)) throw TickerNotFoundException(symbol)
        // Not a session problem: keep the session, so the next request is not three.
        if (res.status == 429) throw RateLimitedException()
        if (res.status != 200) {
            clearSession()
            error("chart HTTP ${res.status}: ${res.body.take(120)}")
        }
        val chart = JSONObject(res.body).getJSONObject("chart")
        if (!chart.isNull("error")) error("chart error: ${chart.get("error")}")
        return chart
    }

    private fun clearSession() {
        cookie = null
        crumb = null
    }

    /** Unknown symbols come back as HTTP 404 with `chart.error.code` = "Not Found". */
    private fun isNotFound(body: String): Boolean = runCatching {
        JSONObject(body).getJSONObject("chart").getJSONObject("error").getString("code") == "Not Found"
    }.getOrDefault(false)

    private fun ensureSession() {
        if (cookie != null && crumb != null) return
        // 404 is expected here; we only want the Set-Cookie header.
        val seed = http.get("https://fc.yahoo.com")
        if (seed.status == 429) throw RateLimitedException()
        val c = seed.setCookies.joinToString("; ") { it.substringBefore(';') }
        if (c.isEmpty()) error("no cookie from fc.yahoo.com (HTTP ${seed.status})")
        val res = http.get("https://query1.finance.yahoo.com/v1/test/getcrumb", mapOf("Cookie" to c))
        if (res.status == 429) throw RateLimitedException()
        if (res.status != 200 || res.body.isBlank()) error("crumb HTTP ${res.status}: ${res.body.take(120)}")
        cookie = c
        crumb = res.body.trim()
    }

    private companion object {
        val AUTH_FAILURES = setOf(401, 403)

        /**
         * Yahoo's `range` and the bar `interval` that fits it: 5D has half-hour bars (79 clean bars; daily would be 5
         * points), then daily up to 1Y, 5Y weekly and Max monthly (decisions 0022, 0027).
         */
        val HistoryRange.yahoo: Pair<String, String>
            get() = when (this) {
                HistoryRange.DAY_1 -> "1d" to "5m"
                HistoryRange.DAYS_5 -> "5d" to "30m"
                HistoryRange.MONTH_1 -> "1mo" to "1d"
                HistoryRange.MONTHS_3 -> "3mo" to "1d"
                HistoryRange.MONTHS_6 -> "6mo" to "1d"
                HistoryRange.YEAR_1 -> "1y" to "1d"
                HistoryRange.YEARS_5 -> "5y" to "1wk"
                HistoryRange.MAX -> "max" to "1mo"
            }

        // URLEncoder is form encoding: fix the space, which belongs as %20 in both path and query.
        fun encode(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
    }
}
