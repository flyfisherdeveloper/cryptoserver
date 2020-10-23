package com.scanner.cryptoserver.exchange.bittrex.service

import com.scanner.cryptoserver.exchange.coinmarketcap.CoinMarketCapService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths


@SpringBootTest
internal class BittrexServiceImplIntegTest(@Autowired private val service: BittrexServiceImpl, @Autowired private val coinMarketCapService: CoinMarketCapService) {

    @Test
    fun `test that get24HrAllCoinTicker() returns valid data`() {
        val coins = service.get24HrAllCoinTicker()
        //find one of the coins that we know should be there
        val mtlBtc = coins.first { it.symbol == "MTL-BTC" }
        println(mtlBtc)
        assertNotNull(mtlBtc.symbol)
        assertEquals("https://bittrex.com/Market/Index?MarketName=BTC-MTL", mtlBtc.tradeLink)
    }

    @Test
    fun `test getBittrexIcons()`() {
        val coins = service.get24HrAllCoinTicker()
        val set = coins.map { it.id }.toCollection(HashSet<Int>())
        val listing = coinMarketCapService.getCoinMarketCapInfoListing(set)
        val logoList = listing.data.values.map { it.logo }

        logoList.forEach {
            val last = it.lastIndexOf('/')
            val fileName = it.substring(last + 1)
            val path = "C:/dev/icons/coin-market-cap-downloaded2/$fileName"
            println("processing: $path")
            URL(it).openStream().use { inputStream -> Files.copy(inputStream, Paths.get(path)) }
        }
    }
}