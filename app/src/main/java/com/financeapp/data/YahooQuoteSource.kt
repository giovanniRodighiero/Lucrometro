package com.financeapp.data

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
        ensureSession()
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol" +
            "?range=1d&interval=1d&crumb=${crumb}"
        val res = http.get(url, mapOf("Cookie" to cookie.orEmpty(), "Accept" to "application/json"))
        if (res.status != 200) {
            cookie = null; crumb = null
            error("chart HTTP ${res.status}: ${res.body.take(120)}")
        }
        val meta = JSONObject(res.body).getJSONObject("chart").getJSONArray("result")
            .getJSONObject(0).getJSONObject("meta")
        Quote(
            symbol = meta.getString("symbol"),
            price = meta.getDouble("regularMarketPrice"),
            currency = meta.optString("currency").ifEmpty { null },
            exchange = meta.optString("exchangeName").ifEmpty { null },
            marketTimeEpoch = meta.getLong("regularMarketTime"),
        )
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
