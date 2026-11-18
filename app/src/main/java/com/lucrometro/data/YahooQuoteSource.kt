package com.lucrometro.data

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
        val meta = chart(symbol, "1d").optJSONArray("result")?.optJSONObject(0)?.optJSONObject("meta")
            ?: throw NoPriceException(symbol)
        if (!meta.has("regularMarketPrice") || meta.isNull("regularMarketPrice") || !meta.has("regularMarketTime")) {
            throw NoPriceException(symbol)
        }
        Quote(
            symbol = meta.optString("symbol").ifEmpty { symbol },
            price = meta.getDouble("regularMarketPrice"),
            currency = meta.optString("currency").ifEmpty { null },
            exchange = meta.optString("exchangeName").ifEmpty { null },
            marketTimeEpoch = meta.getLong("regularMarketTime"),
            name = meta.optString("longName").ifEmpty { meta.optString("shortName") }.ifEmpty { null },
            // With range=1d this is the close of the session before the last one.
            previousClose = meta.optDouble("chartPreviousClose").takeIf { !it.isNaN() },
            priceDecimals = meta.optInt("priceHint", -1).takeIf { it in 0..6 },
        )
    }

    @Synchronized
    override fun history(symbol: String, range: HistoryRange): Result<List<Candle>> = runCatching {
        val result = chart(symbol, range.yahoo).getJSONArray("result").getJSONObject(0)
        val ts = result.getJSONArray("timestamp")
        val q = result.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0)
        fun d(name: String, i: Int): Double? =
            q.optJSONArray(name)?.takeIf { !it.isNull(i) }?.getDouble(i)
        val adj = result.getJSONObject("indicators").optJSONArray("adjclose")?.optJSONObject(0)?.optJSONArray("adjclose")
        (0 until ts.length()).mapNotNull { i ->
            val close = d("close", i) ?: return@mapNotNull null // halted/empty bar
            Candle(
                epochSeconds = ts.getLong(i),
                open = d("open", i),
                high = d("high", i),
                low = d("low", i),
                close = close,
                volume = q.optJSONArray("volume")?.takeIf { !it.isNull(i) }?.getLong(i),
                adjClose = adj?.takeIf { !it.isNull(i) }?.getDouble(i),
            )
        }
    }

    /**
     * Returns the `chart` object of the response; throws on HTTP or API errors. A reused session that
     * Yahoo rejects (expired cookie or crumb: 401/403) is renewed and the request retried once.
     */
    private fun chart(symbol: String, range: String): JSONObject {
        val reused = cookie != null && crumb != null
        val res = fetchChart(symbol, range)
        if (reused && res.status in AUTH_FAILURES) {
            clearSession()
            return parseChart(symbol, fetchChart(symbol, range))
        }
        return parseChart(symbol, res)
    }

    private fun fetchChart(symbol: String, range: String): HttpResult {
        ensureSession()
        // Codes may hold ^ or = (indices, currencies), and the crumb any character: encode both.
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/${encode(symbol)}" +
            "?range=$range&interval=1d&crumb=${encode(crumb.orEmpty())}"
        return http.get(url, mapOf("Cookie" to cookie.orEmpty(), "Accept" to "application/json"))
    }

    private fun parseChart(symbol: String, res: HttpResult): JSONObject {
        if (res.status == 404 && isNotFound(res.body)) throw TickerNotFoundException(symbol)
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
        val c = seed.setCookies.joinToString("; ") { it.substringBefore(';') }
        if (c.isEmpty()) error("no cookie from fc.yahoo.com (HTTP ${seed.status})")
        val res = http.get("https://query1.finance.yahoo.com/v1/test/getcrumb", mapOf("Cookie" to c))
        if (res.status != 200 || res.body.isBlank()) error("crumb HTTP ${res.status}: ${res.body.take(120)}")
        cookie = c
        crumb = res.body.trim()
    }

    private companion object {
        val AUTH_FAILURES = setOf(401, 403)

        // URLEncoder is form encoding: fix the space, which belongs as %20 in both path and query.
        fun encode(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
    }
}
