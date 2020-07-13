package com.scanner.cryptoserver.exchange.bittrex.service

import com.scanner.cryptoserver.CachingConfig
import com.scanner.cryptoserver.exchange.coinmarketcap.CoinMarketCapApiServiceImpl
import com.scanner.cryptoserver.exchange.coinmarketcap.CoinMarketCapService
import com.scanner.cryptoserver.util.CacheUtilImpl
import com.scanner.cryptoserver.util.UrlReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.client.RestTemplate

//Here, we load only the components needed - this prevents a full Spring Boot test from running, as only certain components are needed.
//For example, startup initialization threads are not needed, etc.
@ContextConfiguration(classes = [CachingConfig::class, CacheUtilImpl::class, CoinMarketCapService::class,
    CoinMarketCapApiServiceImpl::class, RestTemplate::class, UrlReader::class, BittrexServiceImpl::class])
@WebMvcTest
internal class BittrexServiceImplIntegTest(@Autowired private val service: BittrexServiceImpl) {

    @Test
    fun testMarkets() {
        val coins = service.get24HrAllCoinTicker()
        //find one of the coins that we know should be there
        val mtlBtc = coins.first { it.symbol == "MTL-BTC" }
        println(mtlBtc)
        assertNotNull(mtlBtc.symbol)
        assertEquals("https://bittrex.com/Market/Index?MarketName=MTL-BTC", mtlBtc.tradeLink)
    }
}