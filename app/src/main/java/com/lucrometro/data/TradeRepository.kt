package com.lucrometro.data

import kotlinx.coroutines.flow.Flow

/** Outcome of a change to a ticker's trades. */
sealed interface TradeChange {
    data object Saved : TradeChange

    /** Refused: replaying the trades after the change takes the shares below 0 at [Replay.BelowZero.trade]. */
    data class BelowZero(val replay: Replay.BelowZero) : TradeChange

    /** Storage failed, or the ticker was removed meanwhile. Nothing changed. */
    data object Failed : TradeChange
}

/**
 * The trades of each ticker (decisions 0041, 0042). Every change is checked by replaying the ticker's trades with it:
 * one that would take the shares below 0 at any point is refused, so the log stays a possible history.
 */
class TradeRepository(private val store: WatchlistStore) {
    fun trades(code: String): Flow<List<Trade>> = store.observeTrades(code)

    /** Adds (id 0) or replaces (same id) [trade]. */
    suspend fun save(trade: Trade): TradeChange = change(trade.code, listOf(trade)) { list -> list.filter { it.id != trade.id } + trade }

    /** Deletes [trade]; undo it with [save], which puts it back with the same id. */
    suspend fun delete(trade: Trade): TradeChange {
        val result = check(trade.code) { list -> list.filter { it.id != trade.id } } ?: return TradeChange.Failed
        if (result != TradeChange.Saved) return result
        return if (storeCall { store.deleteTrade(trade.id) } != null) TradeChange.Saved else TradeChange.Failed
    }

    /** Adds [new] trades of [code] in one go (the CSV import), after the same check. */
    suspend fun addAll(code: String, new: List<Trade>): TradeChange = change(code, new) { it + new }

    /** Saves [toSave] if the trades after [edit] (the same change, applied to the stored list) pass the check. */
    private suspend fun change(code: String, toSave: List<Trade>, edit: (List<Trade>) -> List<Trade>): TradeChange {
        val result = check(code, edit) ?: return TradeChange.Failed
        if (result != TradeChange.Saved) return result
        return if (storeCall { store.saveTrades(code, toSave) } == true) TradeChange.Saved else TradeChange.Failed
    }

    /** [TradeChange.Saved] if the trades of [code] after [edit] replay without going below 0; null if storage failed. */
    private suspend fun check(code: String, edit: (List<Trade>) -> List<Trade>): TradeChange? {
        val current = storeCall { store.trades(code) } ?: return null
        return when (val replay = replay(edit(current))) {
            is Replay.Ok -> TradeChange.Saved
            is Replay.BelowZero -> TradeChange.BelowZero(replay)
        }
    }
}
