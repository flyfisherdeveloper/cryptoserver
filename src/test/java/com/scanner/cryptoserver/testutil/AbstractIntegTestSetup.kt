package com.scanner.cryptoserver.testutil

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.scanner.cryptoserver.CachingConfig
import com.scanner.cryptoserver.exchange.binance.controller.BinanceExchangeController
import com.scanner.cryptoserver.exchange.binance.service.AbstractBinanceExchangeService
import com.scanner.cryptoserver.exchange.binance.service.BinanceExchangeServiceImpl
import com.scanner.cryptoserver.exchange.binance.service.BinanceExchangeVisitor
import com.scanner.cryptoserver.exchange.binance.service.BinanceUrlExtractor
import com.scanner.cryptoserver.exchange.bittrex.service.BittrexServiceImpl
import com.scanner.cryptoserver.exchange.coinmarketcap.CoinMarketCapApiServiceImpl
import com.scanner.cryptoserver.exchange.coinmarketcap.CoinMarketCapService
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapData
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapListing
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.ExchangeInfo
import com.scanner.cryptoserver.util.CacheUtilImpl
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.junit.Before
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.client.RestTemplate
import kotlin.test.BeforeTest

//Here, we load only the components needed - this prevents a full Spring Boot test from running, as only certain components are needed.
//For example, startup initialization threads are not needed, etc.
@ContextConfiguration(classes = [BinanceExchangeController::class, BinanceExchangeServiceImpl::class, BinanceUrlExtractor::class,
    RestTemplate::class, CachingConfig::class, CacheUtilImpl::class, CoinMarketCapApiServiceImpl::class, CoinMarketCapService::class,
    BinanceExchangeVisitor::class, BittrexServiceImpl::class])
abstract class AbstractIntegTestSetup {
    @Autowired
    private val binanceUsaService: AbstractBinanceExchangeService? = null

    @Autowired
    private val binanceService: AbstractBinanceExchangeService? = null

    @Autowired
    private val bittrexService: BittrexServiceImpl? = null

    private var listing: CoinMarketCapListing? = null

    /**
     * Convienience class used only for deserializing Json.
     */
    private class CoinMarketCapObj {
        val data: List<CoinMarketCapData>? = null
    }

    @BeforeEach
    suspend fun setupExchangeInfo() {
        if (listing == null) {
            return
        }
        val text = "/exchange/data/coinmarketcap-map.txt".getResourceAsText()
        val mapper = jacksonObjectMapper()
        val obj = mapper.readValue(text) as CoinMarketCapObj
        listing = CoinMarketCapListing().convertToCoinMarketCapListing(obj.data)
        setupServices()
    }

    private suspend fun setupServices() = coroutineScope {
        //Asynchronously get the exchange information on startup.
        //Do not include the calls that fill the market cap, since that data hasn't been retrieved yet,
        //and will be retrieved when the threads finish retrieving the exchange info.
        //We do this since we need a list of coins to retrieve the market cap info,
        //and the exchange info gives us a list of coins that is needed.

        val binanceExchangeInfo: Deferred<ExchangeInfo> = async { binanceService!!.exchangeInfoWithoutMarketCap }
        val binanceUsaExchangeInfo: Deferred<ExchangeInfo> = async { binanceUsaService!!.exchangeInfoWithoutMarketCap }
        val bittrexExchangeInfo: Deferred<ExchangeInfo> = async { bittrexService!!.exchangeInfo }
        continueSetup(binanceExchangeInfo.await(), binanceUsaExchangeInfo.await(), bittrexExchangeInfo.await())
    }

    private fun continueSetup(binanceExchangeInfo: ExchangeInfo, binanceUsaExchangeInfo: ExchangeInfo, bittrexExchangeInfo: ExchangeInfo) {
        binanceExchangeInfo.coins.forEach { it.addMarketCapAndId(binanceService!!.exchangeVisitor, listing) }
        binanceUsaExchangeInfo.coins.forEach { it.addMarketCapAndId(binanceUsaService!!.exchangeVisitor, listing) }
        bittrexExchangeInfo.coins.forEach { it.addMarketCapAndId(binanceService!!.exchangeVisitor, listing) }
    }

    private fun String.getResourceAsText(): String {
        return object {}.javaClass.getResource(this).readText()
    }

    fun getListing(): CoinMarketCapListing? {
        return listing
    }

    fun getBinanceService(): AbstractBinanceExchangeService {
        return binanceService!!
    }

    fun getBinanceUsaService(): AbstractBinanceExchangeService {
        return binanceUsaService!!
    }

    fun getBittrexService(): BittrexServiceImpl {
        return bittrexService!!
    }
}