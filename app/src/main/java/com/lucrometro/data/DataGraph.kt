package com.lucrometro.data

import android.content.Context
import com.lucrometro.data.db.LucrometroDatabase
import com.lucrometro.data.db.RoomWatchlistStore

/**
 * Process-wide data objects, created on first use. The only place that picks the concrete
 * providers (Yahoo via Cronet) and the storage (Room), so the UI never sees them.
 */
object DataGraph {
    @Volatile private var watchlist: WatchlistRepository? = null

    fun watchlist(context: Context): WatchlistRepository =
        watchlist ?: synchronized(this) {
            watchlist ?: WatchlistRepository(
                source = YahooQuoteSource(CronetHttp(context.applicationContext)),
                store = RoomWatchlistStore(LucrometroDatabase.create(context).watchlist()),
            ).also { watchlist = it }
        }
}
