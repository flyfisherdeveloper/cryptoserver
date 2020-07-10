package com.scanner.cryptoserver.exchange.bittrex.service

import com.scanner.cryptoserver.CachingConfig
import com.scanner.cryptoserver.util.CacheUtilImpl
import junit.framework.Assert.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.ContextConfiguration

//Here, we load only the components needed - this prevents a full Spring Boot test from running, as only certain components are needed.
//For example, startup initialization threads are not needed, etc.
@ContextConfiguration(classes = [CachingConfig::class, CacheUtilImpl::class, BittrexServiceImpl::class])
@WebMvcTest
internal class BittrexServiceImplIntegTest(@Autowired private val service: BittrexServiceImpl) {

    @Test
    fun testMarkets() {
        val coins = service.get24HrAllCoinTicker()
        //find one of the coins that we know should be there
        val mtlBtc = coins.first { it.symbol == "MTL-BTC" }
        println(mtlBtc)
        assertNotNull(mtlBtc.symbol)
    }
}