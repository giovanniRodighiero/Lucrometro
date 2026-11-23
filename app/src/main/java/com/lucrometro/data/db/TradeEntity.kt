package com.lucrometro.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.lucrometro.data.Side
import com.lucrometro.data.Trade
import java.time.LocalDate
import java.time.LocalTime

/** One trade (decision 0041). Date and time as ISO text ("2026-02-01", "10:00:00"), the time in the exchange's clock. */
@Entity(tableName = "trade", indices = [Index("code")])
data class TradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val code: String,
    val side: Side,
    val date: String,
    val time: String?,
    val quantity: Double,
    val price: Double,
    val brokerId: String?,
)

internal fun TradeEntity.toTrade() =
    Trade(id, code, side, LocalDate.parse(date), time?.let(LocalTime::parse), quantity, price, brokerId)

internal fun Trade.toEntity() = TradeEntity(id, code, side, date.toString(), time?.toString(), quantity, price, brokerId)
