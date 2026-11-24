package com.lucrometro.data

/** Saved year ends per ticker (decisions 0051, 0052). Room implements it in `data/db`; tests use a fake. */
interface YearEndStore {
    /** What was saved for [code], or null if it was never fetched. */
    suspend fun get(code: String): TickerYears?

    /** Replaces what was saved for [TickerYears.code]. */
    suspend fun save(years: TickerYears)
}
