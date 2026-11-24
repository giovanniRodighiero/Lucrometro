package com.lucrometro.data.db

import com.lucrometro.data.TickerYears
import com.lucrometro.data.YearEndStore

class RoomYearEndStore(private val dao: YearEndDao) : YearEndStore {
    override suspend fun get(code: String): TickerYears? {
        val fetch = dao.fetch(code) ?: return null
        return TickerYears(code, fetch.fromYear, fetch.throughYear, dao.ends(code).map { it.toYearEnd() })
    }

    override suspend fun save(years: TickerYears) = dao.replace(
        YearEndFetchEntity(years.code, years.fromYear, years.throughYear),
        years.ends.map { it.toEntity(years.code) },
    )
}
