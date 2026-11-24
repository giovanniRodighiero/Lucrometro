package com.lucrometro.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException
import java.util.Locale

/** Why a trade CSV (decision 0041) was refused. [line] counts from 1, the header included. */
sealed interface CsvError {
    data class Missing(val column: String) : CsvError
    data class Columns(val line: Int) : CsvError
    data class BadSide(val line: Int, val value: String) : CsvError
    data class BadDate(val line: Int, val value: String) : CsvError
    data class BadTime(val line: Int, val value: String) : CsvError
    data class NotANumber(val line: Int, val column: String, val value: String) : CsvError
    data class NotPositive(val line: Int, val column: String) : CsvError
}

sealed interface CsvParse {
    /** Every row of the file, for any ticker, in file order. */
    data class Ok(val rows: List<Trade>) : CsvParse
    data class Bad(val error: CsvError) : CsvParse
}

private val REQUIRED = listOf("ticker", "side", "date", "quantity", "price")
private val NUMBER = Regex("""\d+(\.\d+)?""")

/**
 * Parses the app's trade CSV (decision 0041): UTF-8 (a BOM is allowed), `,` separators, `.` decimals, a header row
 * matched by name in any order, unknown columns ignored, empty lines skipped. A field may be quoted ("..." with "" for a
 * quote). The first bad row stops it: nothing is half-read.
 */
fun parseTradeCsv(text: String): CsvParse {
    val lines = text.removePrefix("﻿").lines().map { it.removeSuffix("\r") }
    val headerAt = lines.indexOfFirst { it.isNotBlank() }
    if (headerAt < 0) return CsvParse.Bad(CsvError.Missing(REQUIRED.first()))
    val header = splitCsvLine(lines[headerAt]).map { it.trim().lowercase(Locale.ROOT) }
    REQUIRED.firstOrNull { it !in header }?.let { return CsvParse.Bad(CsvError.Missing(it)) }
    fun col(name: String) = header.indexOf(name)

    val rows = mutableListOf<Trade>()
    for (i in headerAt + 1 until lines.size) {
        if (lines[i].isBlank()) continue
        val line = i + 1
        val fields = splitCsvLine(lines[i]).map { it.trim() }
        fun field(name: String): String? = col(name).takeIf { it >= 0 }?.let { fields.getOrNull(it) }
        if (REQUIRED.any { field(it) == null }) return CsvParse.Bad(CsvError.Columns(line))

        val side = when (field("side")!!.lowercase(Locale.ROOT)) {
            "buy" -> Side.BUY
            "sell" -> Side.SELL
            else -> return CsvParse.Bad(CsvError.BadSide(line, field("side")!!))
        }
        val date = try {
            LocalDate.parse(field("date")!!)
        } catch (e: DateTimeParseException) {
            return CsvParse.Bad(CsvError.BadDate(line, field("date")!!))
        }
        val time = field("time")?.takeIf { it.isNotEmpty() }?.let {
            try {
                LocalTime.parse(it)
            } catch (e: DateTimeParseException) {
                return CsvParse.Bad(CsvError.BadTime(line, it))
            }
        }
        val numbers = listOf("quantity", "price").map { name ->
            val value = field(name)!!
            if (!NUMBER.matches(value)) return CsvParse.Bad(CsvError.NotANumber(line, name, value))
            value.toDouble().takeIf { it > 0 } ?: return CsvParse.Bad(CsvError.NotPositive(line, name))
        }
        rows += Trade(
            code = normalizeTicker(field("ticker")!!),
            side = side,
            date = date,
            time = time,
            quantity = numbers[0],
            price = numbers[1],
            brokerId = field("id")?.takeIf { it.isNotEmpty() },
        )
    }
    return CsvParse.Ok(rows)
}

/** One CSV line into fields: commas separate, a field in double quotes may hold commas and "" for a quote. */
private fun splitCsvLine(line: String): List<String> {
    val fields = mutableListOf<String>()
    val current = StringBuilder()
    var quoted = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when {
            quoted && c == '"' && line.getOrNull(i + 1) == '"' -> { current.append('"'); i++ }
            c == '"' -> quoted = !quoted
            c == ',' && !quoted -> { fields += current.toString(); current.clear() }
            else -> current.append(c)
        }
        i++
    }
    fields += current.toString()
    return fields
}

/** What importing a file would do to one ticker (decision 0042). */
sealed interface ImportCheck {
    data class Ready(
        val new: List<Trade>,
        /** Rows for other tickers, skipped. */
        val otherTickers: Int,
        /** Rows whose id is already saved (or repeated in the file), skipped. */
        val alreadySaved: Int,
        val after: Holding,
    ) : ImportCheck

    data object NoRows : ImportCheck

    /** Every row for the ticker is already saved. */
    data object NothingNew : ImportCheck

    /** With the new trades the shares go below 0: older trades are missing from the file. */
    data class BelowZero(val replay: Replay.BelowZero) : ImportCheck

    /** [trade] is dated after today: a typo, since no trade happens in the future. */
    data class Future(val trade: Trade) : ImportCheck
}

/** Picks [code]'s new rows out of a parsed file and checks them against the [saved] trades; [today] in the exchange's zone. */
fun checkImport(code: String, rows: List<Trade>, saved: List<Trade>, today: LocalDate): ImportCheck {
    val mine = rows.filter { it.code == code }
    if (mine.isEmpty()) return ImportCheck.NoRows
    val seen = saved.mapNotNull { it.brokerId }.toMutableSet()
    // A row with an id seen before (saved, or earlier in the file) is skipped; rows without an id are always new.
    val new = mine.filter { it.brokerId == null || seen.add(it.brokerId) }
    if (new.isEmpty()) return ImportCheck.NothingNew
    new.firstOrNull { it.date.isAfter(today) }?.let { return ImportCheck.Future(it) }
    return when (val replay = replay(saved + new)) {
        is Replay.BelowZero -> ImportCheck.BelowZero(replay)
        is Replay.Ok -> ImportCheck.Ready(new, rows.size - mine.size, mine.size - new.size, replay.holding)
    }
}

/** What importing a file with mixed tickers would do (decision 0054). */
sealed interface GlobalImportCheck {
    data class Ready(
        /** Each watchlist ticker with new trades, in watchlist order. */
        val tickers: List<Pair<String, ImportCheck.Ready>>,
        /** Row count of each ticker not on the watchlist, in file order: skipped. */
        val notInWatchlist: Map<String, Int>,
        /** Rows of watchlist tickers whose id is already saved (or repeated in the file), skipped. */
        val alreadySaved: Int,
    ) : GlobalImportCheck

    /** No row is for a watchlist ticker. */
    data object NoWatchlistRows : GlobalImportCheck

    /** Every row for a watchlist ticker is already saved. */
    data object NothingNew : GlobalImportCheck

    /** One ticker's shares would go below 0 ([Replay.BelowZero.trade] names it): nothing is saved. */
    data class BelowZero(val replay: Replay.BelowZero) : GlobalImportCheck

    /** [trade] is dated after today: nothing is saved. */
    data class Future(val trade: Trade) : GlobalImportCheck
}

/**
 * [checkImport] for every [watchlist] ticker (in its order) with rows in the file; [saved] is every ticker's trades,
 * [today] gives the date in a ticker's exchange zone. The first ticker that fails fails the whole file.
 */
fun checkGlobalImport(watchlist: List<String>, rows: List<Trade>, saved: List<Trade>, today: (String) -> LocalDate): GlobalImportCheck {
    val savedByCode = saved.groupBy { it.code }
    val ready = mutableListOf<Pair<String, ImportCheck.Ready>>()
    var alreadySaved = 0
    for (code in watchlist) {
        when (val check = checkImport(code, rows, savedByCode[code].orEmpty(), today(code))) {
            ImportCheck.NoRows -> {}
            ImportCheck.NothingNew -> alreadySaved += rows.count { it.code == code }
            is ImportCheck.Ready -> {
                ready += code to check
                alreadySaved += check.alreadySaved
            }
            is ImportCheck.BelowZero -> return GlobalImportCheck.BelowZero(check.replay)
            is ImportCheck.Future -> return GlobalImportCheck.Future(check.trade)
        }
    }
    return when {
        ready.isNotEmpty() -> GlobalImportCheck.Ready(
            ready,
            rows.filter { it.code !in watchlist }.groupingBy { it.code }.eachCount(),
            alreadySaved,
        )
        alreadySaved > 0 -> GlobalImportCheck.NothingNew
        else -> GlobalImportCheck.NoWatchlistRows
    }
}
