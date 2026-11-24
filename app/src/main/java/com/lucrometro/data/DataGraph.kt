package com.lucrometro.data

import android.content.Context
import com.lucrometro.data.db.LucrometroDatabase
import com.lucrometro.data.db.RoomWatchlistStore
import com.lucrometro.data.db.RoomYearEndStore

/**
 * Process-wide data objects, created on first use. The only place that picks the concrete
 * providers (Yahoo via Cronet) and the storage (Room), so the UI never sees them.
 */
object DataGraph {
    private class Graph(context: Context) {
        private val source = YahooQuoteSource(CronetHttp(context))
        private val gate = SourceGate()
        private val database = LucrometroDatabase.create(context)
        private val store = RoomWatchlistStore(database.watchlist())
        val watchlist = WatchlistRepository(source, store, gate)
        val trades = TradeRepository(store)
        val history = HistoryRepository(source, gate, watchlist)
        val portfolio = PortfolioRepository(source, gate, store, RoomYearEndStore(database.yearEnds()), watchlist)
    }

    @Volatile private var graph: Graph? = null

    private fun graph(context: Context): Graph =
        graph ?: synchronized(this) { graph ?: Graph(context.applicationContext).also { graph = it } }

    fun watchlist(context: Context): WatchlistRepository = graph(context).watchlist

    fun history(context: Context): HistoryRepository = graph(context).history

    fun trades(context: Context): TradeRepository = graph(context).trades

    fun portfolio(context: Context): PortfolioRepository = graph(context).portfolio
}
