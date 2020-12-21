package com.scanner.cryptoserver.exchange.bittrex.service

import com.scanner.cryptoserver.testutil.AbstractIntegTestSetup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class BittrexServiceImplIntegTest : AbstractIntegTestSetup() {

    @Test
    fun `test that get24HrAllCoinTicker() returns valid data`() {
        val coins = getBittrexService()!!.get24HrAllCoinTicker()
        //find one of the coins that we know should be there
        val mtlBtc = coins.first { it.symbol == "MTL-BTC" }
        println(mtlBtc)
        assertNotNull(mtlBtc.symbol)
        assertEquals("https://bittrex.com/Market/Index?MarketName=BTC-MTL", mtlBtc.tradeLink)
    }
}