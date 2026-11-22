package com.lucrometro.data.db

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.lucrometro.data.Position

/** The owner's average price for a ticker (decision 0040): one row per ticker, replaced on every save. */
@Entity(tableName = "position")
data class PositionEntity(
    @PrimaryKey val code: String,
    val averagePrice: Double,
)

internal fun PositionEntity.toPosition() = Position(averagePrice)

internal fun Position.toEntity(code: String) = PositionEntity(code, averagePrice)
