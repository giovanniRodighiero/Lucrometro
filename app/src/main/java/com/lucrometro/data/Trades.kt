package com.lucrometro.data

import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs

enum class Side { BUY, SELL }

/**
 * One trade in a ticker (decisions 0040, 0041): typed in or imported, the same thing. [time] is in the exchange's clock,
 * null when unknown. [price] is per share, without fees. [brokerId] is the broker's order number from a CSV, if any.
 */
data class Trade(
    val id: Long = 0,
    val code: String,
    val side: Side,
    val date: LocalDate,
    val time: LocalTime?,
    val quantity: Double,
    val price: Double,
    val brokerId: String? = null,
) {
    /** Quantity times price, no fees. */
    val amount: Double get() = quantity * price
}

/**
 * A gross realized gain (decision 0043): [gain] = (sell price - average then) x shares sold, [cost] = those shares at
 * the average then, so [percent] is the gain over what the sold shares cost.
 */
data class Realized(val gain: Double, val cost: Double) {
    val percent: Double get() = gain / cost * 100

    operator fun plus(other: Realized) = Realized(gain + other.gain, cost + other.cost)
}

/**
 * What a ticker's trades add up to: [average] is null at 0 shares (sold out, or no trades). [sells] is each sell's
 * realized gain by trade id (stored trades: ids are unique), [realized] their total, null with no sells; both survive a
 * sell-out.
 */
data class Holding(
    val shares: Double,
    val average: Double?,
    val count: Int,
    val lastBuy: Trade?,
    val sells: Map<Long, Realized> = emptyMap(),
    val realized: Realized? = null,
)

sealed interface Replay {
    data class Ok(val holding: Holding) : Replay

    /** [trade] takes the shares below 0: there were only [sharesBefore] just before it. */
    data class BelowZero(val trade: Trade, val sharesBefore: Double) : Replay
}

// Shares can have decimals (2.5): sums of doubles are compared with this tolerance.
private const val EPSILON = 1e-9

/**
 * Oldest first on the trade's day and time. A trade without a time is unknown within its day: a buy counts at the start
 * of the day and a sell at its end, so a missing time never makes a complete history look short of shares.
 */
val TRADE_ORDER: Comparator<Trade> = compareBy<Trade>({ it.date }, { it.time ?: if (it.side == Side.BUY) LocalTime.MIN else LocalTime.MAX }, { it.id })

/**
 * Replays [trades] oldest first with the weighted average cost without fees (decision 0041): a buy moves the average, a
 * sell keeps it and realizes a gain against it, and at 0 shares it starts again. Stops at the first trade that would take the shares below 0.
 */
fun replay(trades: List<Trade>): Replay {
    var shares = 0.0
    var average = 0.0
    var lastBuy: Trade? = null
    val sells = HashMap<Long, Realized>()
    var realized: Realized? = null
    for (t in trades.sortedWith(TRADE_ORDER)) {
        when (t.side) {
            Side.BUY -> {
                average = (average * shares + t.price * t.quantity) / (shares + t.quantity)
                shares += t.quantity
                lastBuy = t
            }
            Side.SELL -> {
                if (t.quantity > shares + EPSILON) return Replay.BelowZero(t, shares)
                val sell = Realized((t.price - average) * t.quantity, average * t.quantity)
                sells[t.id] = sell
                realized = realized?.plus(sell) ?: sell
                shares -= t.quantity
                if (abs(shares) < EPSILON) {
                    shares = 0.0
                    average = 0.0
                }
            }
        }
    }
    return Replay.Ok(Holding(shares, average.takeIf { shares > 0 }, trades.size, lastBuy, sells, realized))
}
