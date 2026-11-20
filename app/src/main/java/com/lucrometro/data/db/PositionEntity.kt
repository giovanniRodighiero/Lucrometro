package com.lucrometro.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.lucrometro.data.Position
import java.time.LocalDate

/** One saved position entry (decision 0033): entries are never overwritten, the latest by [id] is the position. */
@Entity(tableName = "position", indices = [Index("code")])
data class PositionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val averagePrice: Double,
    val shares: Double?,
    /** [LocalDate.toEpochDay] of the last buy. */
    val lastBuyDay: Long,
    /** Epoch millis when the entry was saved. */
    val savedAt: Long,
)

internal fun PositionEntity.toPosition() = Position(averagePrice, shares, LocalDate.ofEpochDay(lastBuyDay), savedAt)

internal fun Position.toEntity(code: String) = PositionEntity(
    code = code, averagePrice = averagePrice, shares = shares, lastBuyDay = lastBuy.toEpochDay(), savedAt = savedAt,
)
