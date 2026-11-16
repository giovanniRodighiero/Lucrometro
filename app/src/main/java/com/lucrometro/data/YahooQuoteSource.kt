package com.lucrometro.data

import org.json.JSONObject

/**
 * Unofficial Yahoo Finance source. Flow: cookie (fc.yahoo.com) -> crumb -> chart endpoint.
 * Yahoo rejects non-browser TLS fingerprints, hence Cronet.
 */
class YahooQuoteSource(private val http: CronetHttp) : QuoteSource {
    private var cookie: String? = null
    private var crumb: String? = null

    @Synchronized
    override fun quote(symbol: String): Result<Quote> = runCatching {
        val meta = chart(symbol, "1d").getJSONArray("result").getJSONObject(0).getJSONObject("meta")
        Quote(
            symbol = meta.getString("symbol"),
            price = meta.getDouble("regularMarketPrice"),
            currency = meta.optString("currency").ifEmpty { null },
            exchange = meta.optString("exchangeName").ifEmpty { null },
            marketTimeEpoch = meta.getLong("regularMarketTime"),
        )
    }

    @Synchronized
    override fun history(symbol: String, range: HistoryRange): Result<List<Candle>> = runCatching {
        val result = chart(symbol, range.yahoo).getJSONArray("result").getJSONObject(0)
        val ts = result.getJSONArray("timestamp")
        val q = result.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0)
        fun d(name: String, i: Int): Double? =
            q.optJSONArray(name)?.takeIf { !it.isNull(i) }?.getDouble(i)
        (0 until ts.length()).mapNotNull { i ->
            val close = d("close", i) ?: return@mapNotNull null // halted/empty bar
            Candle(
                epochSeconds = ts.getLong(i),
                open = d("open", i),
                high = d("high", i),
                low = d("low", i),
                close = close,
                volume = q.optJSONArray("volume")?.takeIf { !it.isNull(i) }?.getLong(i),
            )
        }
    }

    /** Returns the `chart` object of the response; throws on HTTP or API errors. */
    private fun chart(symbol: String, range: String): JSONObject {
        ensureSession()
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol" +
            "?range=$range&interval=1d&crumb=$crumb"
        val res = http.get(url, mapOf("Cookie" to cookie.orEmpty(), "Accept" to "application/json"))
        if (res.status != 200) {
            cookie = null; crumb = null
            error("chart HTTP ${res.status}: ${res.body.take(120)}")
        }
        val chart = JSONObject(res.body).getJSONObject("chart")
        if (!chart.isNull("error")) error("chart error: ${chart.get("error")}")
        return chart
    }

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
}
