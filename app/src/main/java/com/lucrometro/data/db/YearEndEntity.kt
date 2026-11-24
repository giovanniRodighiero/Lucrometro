package com.lucrometro.data.db

import androidx.room3.Entity
import com.lucrometro.data.YearEnd
import java.time.LocalDate

/**
 * A ticker's last daily bar of a calendar year (decisions 0051, 0052). Market data, not the owner's: kept when the ticker
 * is removed, so adding it back needs no new request. [date] as ISO text, in the exchange's clock.
 */
@Entity(tableName = "year_end", primaryKeys = ["code", "year"])
data class YearEndEntity(
    val code: String,
    val year: Int,
    val date: String,
    val close: Double,
    val adjClose: Double?,
)

/**
 * Which years the saved year ends of a ticker cover: [fromYear] through [throughYear], from one fetch. A year in the
 * range without a row had no bar (the ticker was not listed yet), so it is not fetched again.
 */
@Entity(tableName = "year_end_fetch", primaryKeys = ["code"])
data class YearEndFetchEntity(val code: String, val fromYear: Int, val throughYear: Int)

internal fun YearEndEntity.toYearEnd() = YearEnd(year, LocalDate.parse(date), close, adjClose)

internal fun YearEnd.toEntity(code: String) = YearEndEntity(code, year, date.toString(), close, adjClose)
