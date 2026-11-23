package com.lucrometro.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TradeCsvTest {
    private fun rows(text: String) = (parseTradeCsv(text) as CsvParse.Ok).rows
    private fun error(text: String) = (parseTradeCsv(text) as CsvParse.Bad).error

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
}
