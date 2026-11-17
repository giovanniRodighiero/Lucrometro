package com.lucrometro.data

import android.content.Context

/**
 * Process-wide data objects, created on first use. The only place that picks the concrete
 * providers (Yahoo via Cronet), so the UI never sees them.
 */
object DataGraph {
    @Volatile private var watchlist: WatchlistRepository? = null

    fun watchlist(context: Context): WatchlistRepository =
        watchlist ?: synchronized(this) {
            watchlist ?: WatchlistRepository(YahooQuoteSource(CronetHttp(context.applicationContext)))
                .also { watchlist = it }
        }
}
