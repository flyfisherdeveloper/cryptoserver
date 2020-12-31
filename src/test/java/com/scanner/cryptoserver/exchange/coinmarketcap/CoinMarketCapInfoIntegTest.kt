package com.scanner.cryptoserver.exchange.coinmarketcap

import com.scanner.cryptoserver.util.UrlReaderImpl
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.jupiter.api.Disabled
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.client.RestTemplate

@RunWith(SpringRunner::class)
//Here, we load only the components needed - this prevents a full Spring Boot test from running, as only certain components are needed.
//For example, startup initialization threads are not needed, etc.
@ContextConfiguration(classes = [UrlReaderImpl::class, RestTemplate::class, CoinMarketCapApiServiceImpl::class])
@WebMvcTest
internal class CoinMarketCapInfoIntegTest {
    @Autowired
    lateinit var service: CoinMarketCapApiServiceImpl

    @Disabled
    @Test
    fun `test all coin market cap info by printing data`() {
        val map = service.coinMarketCapMap
        assertNotNull(map)
        val data = map.data.values
        val sortedData = data.sortedBy { it.dateAdded }
        sortedData.forEach { println("symbol: " + it.symbol + " name: '" + it.name + "' date added: " + it.dateAdded) }
    }
}
