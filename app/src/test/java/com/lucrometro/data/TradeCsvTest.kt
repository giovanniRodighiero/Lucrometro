package com.lucrometro.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TradeCsvTest {
    private fun rows(text: String) = (parseTradeCsv(text) as CsvParse.Ok).rows
    private fun error(text: String) = (parseTradeCsv(text) as CsvParse.Bad).error
    private val today = LocalDate.of(2026, 11, 23)
    private fun checkImport(code: String, rows: List<Trade>, saved: List<Trade>) = checkImport(code, rows, saved, today)

    @Test
    fun `reads the documented example with a BOM, columns in any order and unknown ones`() {
        val r = rows("﻿price,side,ticker,date,time,quantity,id,note\r\n14.25,buy,eni.mi,2026-02-01,10:00:00,10,A123,x\r\n\r\n15.10,SELL,ENI.MI,2026-03-15,,4,,\n")
        assertEquals(2, r.size)
        assertEquals(Trade(code = "ENI.MI", side = Side.BUY, date = LocalDate.of(2026, 2, 1), time = LocalTime.of(10, 0), quantity = 10.0, price = 14.25, brokerId = "A123"), r[0])
        assertNull(r[1].time)
        assertNull(r[1].brokerId)
    }

    @Test
    fun `reports the first bad row with its line number`() {
        val head = "ticker,side,date,quantity,price\n"
        assertEquals(CsvError.Missing("price"), error("ticker,side,date,quantity\n"))
        assertEquals(CsvError.NotANumber(3, "price", "41,20"), error(head + "ENI.MI,buy,2026-01-02,1,1\nENI.MI,buy,2026-01-02,1,\"41,20\"\n"))
        assertEquals(CsvError.BadSide(2, "hold"), error(head + "ENI.MI,hold,2026-01-02,1,1\n"))
        assertEquals(CsvError.BadDate(2, "02/01/2026"), error(head + "ENI.MI,buy,02/01/2026,1,1\n"))
        assertEquals(CsvError.NotPositive(2, "quantity"), error(head + "ENI.MI,buy,2026-01-02,0,1\n"))
        assertEquals(CsvError.Columns(2), error(head + "ENI.MI,buy,2026-01-02\n"))
    }

    @Test
    fun `an import skips other tickers and saved ids, and refuses a history short of shares`() {
        val head = "ticker,side,date,quantity,price,id\n"
        val file = rows(head + "ENI.MI,buy,2026-01-02,10,10,A\nISP.MI,buy,2026-01-02,5,3,B\nENI.MI,buy,2026-01-03,10,12,C\nENI.MI,buy,2026-01-03,10,12,C\n")
        val saved = listOf(file[0].copy(id = 1))
        val ready = checkImport("ENI.MI", file, saved) as ImportCheck.Ready
        assertEquals(1, ready.new.size)
        assertEquals(1, ready.otherTickers)
        assertEquals(2, ready.alreadySaved)
        assertEquals(11.0, ready.after.average!!, 1e-9)

        assertEquals(ImportCheck.NothingNew, checkImport("ENI.MI", file.take(1), saved))
        assertEquals(ImportCheck.NoRows, checkImport("UCG.MI", file, saved))
        val sell = rows(head + "ENI.MI,sell,2026-01-01,5,10,\n")
        assert(checkImport("ENI.MI", sell, emptyList()) is ImportCheck.BelowZero)
    }

    @Test
    fun `an import refuses a trade dated after today, and takes today itself`() {
        val head = "ticker,side,date,quantity,price\n"
        val future = rows(head + "ENI.MI,buy,2026-11-23,1,10\nENI.MI,buy,2027-11-23,1,10\n")
        assertEquals(ImportCheck.Future(future[1]), checkImport("ENI.MI", future, emptyList()))
        assert(checkImport("ENI.MI", future.take(1), emptyList()) is ImportCheck.Ready)
    }

    @Test
    fun `a global import keeps watchlist order, counts what it skips, and fails whole on one ticker`() {
        val head = "ticker,side,date,quantity,price,id\n"
        val file = rows(
            head + "ENI.MI,buy,2026-01-02,10,10,A\nCSSPX.MI,buy,2026-01-02,1,500,B\nISP.MI,buy,2026-01-02,5,3,C\n" +
                "ISP.MI,buy,2026-01-03,5,3,C\nENI.MI,buy,2026-01-05,10,12,D\nCSSPX.MI,sell,2026-01-06,1,510,E\n",
        )
        val saved = listOf(file[0].copy(id = 1))
        val watchlist = listOf("ISP.MI", "SWDA.MI", "ENI.MI")
        val ready = checkGlobalImport(watchlist, file, saved) { today } as GlobalImportCheck.Ready
        assertEquals(listOf("ISP.MI", "ENI.MI"), ready.tickers.map { it.first })
        assertEquals(mapOf("CSSPX.MI" to 2), ready.notInWatchlist)
        assertEquals(2, ready.alreadySaved) // ENI.MI's A, and ISP.MI's repeated C

        val short = rows(head + "ISP.MI,buy,2026-01-02,5,3,\nENI.MI,sell,2026-01-02,5,3,\n")
        val below = checkGlobalImport(watchlist, short, emptyList()) { today } as GlobalImportCheck.BelowZero
        assertEquals("ENI.MI", below.replay.trade.code)

        assertEquals(GlobalImportCheck.NothingNew, checkGlobalImport(watchlist, file.take(1), saved) { today })
        assertEquals(GlobalImportCheck.NoWatchlistRows, checkGlobalImport(listOf("UCG.MI"), file, saved) { today })
    }
}
