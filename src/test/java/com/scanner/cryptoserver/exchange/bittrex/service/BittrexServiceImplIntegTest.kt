package com.scanner.cryptoserver.exchange.bittrex.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
internal class BittrexServiceImplIntegTest(@Autowired private val service: BittrexServiceImpl) {

    @Test
    fun testGet24HrAllCoinTicker() {
        val coins = service.get24HrAllCoinTicker()
        //find one of the coins that we know should be there
        val mtlBtc = coins.first { it.symbol == "MTL-BTC" }
        println(mtlBtc)
        assertNotNull(mtlBtc.symbol)
        assertEquals("https://bittrex.com/Market/Index?MarketName=BTC-MTL", mtlBtc.tradeLink)
    }
}