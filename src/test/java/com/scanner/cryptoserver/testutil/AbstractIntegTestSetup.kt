package com.scanner.cryptoserver.testutil

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.scanner.cryptoserver.CachingConfig
import com.scanner.cryptoserver.exchange.binance.controller.BinanceExchangeController
import com.scanner.cryptoserver.exchange.binance.controller.BinanceUsaExchangeController
import com.scanner.cryptoserver.exchange.binance.service.*
import com.scanner.cryptoserver.exchange.bittrex.service.BittrexServiceImpl
import com.scanner.cryptoserver.exchange.coinmarketcap.CoinMarketCapApiServiceImpl
import com.scanner.cryptoserver.exchange.coinmarketcap.CoinMarketCapService
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapData
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapListing
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.ExchangeInfo
import com.scanner.cryptoserver.util.CacheUtilImpl
import com.scanner.cryptoserver.util.UrlReaderImpl
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.client.RestTemplate

//This Abstract class is the parent of integraton tests. It loads only the modules needed to run an integ test. This is
//preferable to running a spring boot test so that only the components and services needed are loaded, as opposed
//to a spring boot test, which loads everything. This parent class does NOT load the coin market cap data - the
//reason for this is that the coin market cap data is severely limited in daily quota without a paying license.
//The daily quota would be reached quickly if everytime an integ test is ran it queried that data.
//Therefore, in this class, a file that contains a sandbox snapshot of the the coin market cap data is loaded
//into memory and is used by the exchanges for the coin market cap data for the integ tests, as opposed to downloading it from the
//coin market cap api. The integ tests still function as usual in this case, and no testing logic is lost with this approach.


//Here, we load only the components needed - this prevents a full Spring Boot test from running, as only certain components are needed.
//For example, startup initialization threads are not needed, etc.
@ContextConfiguration(
    classes = [BinanceExchangeController::class, BinanceUsaExchangeController::class, BinanceExchangeServiceImpl::class,
        BinanceUsaExchangeServiceImpl::class, BinanceUrlExtractor::class,
        RestTemplate::class, CachingConfig::class, CacheUtilImpl::class, CoinMarketCapApiServiceImpl::class, CoinMarketCapService::class,
        BinanceExchangeVisitor::class, BittrexServiceImpl::class, BinanceUsaUrlExtractor::class,
        BinanceUrlExtractor::class, UrlReaderImpl::class]
)
abstract class AbstractIntegTestSetup {
    private val marketCapListing: String = "Listing"
    private val marketCap: String = "MarketCap"
    private val coinMarketCap: String = "CoinMarketCap"

    @Autowired
    private val binanceUsaService: BinanceUsaExchangeServiceImpl? = null

    @Autowired
    private val binanceService: BinanceExchangeServiceImpl? = null

    @Autowired
    private val bittrexService: BittrexServiceImpl? = null

    @Autowired
    private val cacheUtil: CacheUtilImpl? = null

    private var listing: CoinMarketCapListing? = null

    /**
     * Convienience class used only for deserializing Json.
     */
    data class CoinMarketCapObj(var data: List<CoinMarketCapData>)

    @BeforeEach
    fun setupExchangeInfo() {
        if (listing != null) {
            return
        }
        //load a file containing the coin market cap data
        val text = "/exchange/data/coinmarketcap-map.txt".getResourceAsText()
        val mapper = jacksonObjectMapper()
        val obj = mapper.readValue(text) as CoinMarketCapObj
        listing = CoinMarketCapListing().convertToCoinMarketCapListing(obj.data)
        //put that data in the cache
        cacheUtil!!.putInCache(coinMarketCap, marketCap, listing)
        cacheUtil.putInCache(coinMarketCap, marketCapListing, listing)
        setupServices()
    }

    private fun setupServices() {
        //Asynchronously get the exchange information on startup.
        //Do not include the calls that fill the market cap, since that data hasn't been retrieved yet,
        //and will be retrieved when the threads finish retrieving the exchange info.
        //We do this since we need a list of coins to retrieve the market cap info,
        //and the exchange info gives us a list of coins that is needed.
        val async = GlobalScope.async {
            val binanceExchangeInfo: Deferred<ExchangeInfo> = async { binanceService!!.exchangeInfoWithoutMarketCap }
            val binanceUsaExchangeInfo: Deferred<ExchangeInfo> =
                async { binanceUsaService!!.exchangeInfoWithoutMarketCap }
            val bittrexExchangeInfo: Deferred<ExchangeInfo> = async { bittrexService!!.exchangeInfo }
            continueSetup(binanceExchangeInfo.await(), binanceUsaExchangeInfo.await(), bittrexExchangeInfo.await())
        }
    }

    private fun continueSetup(
        binanceExchangeInfo: ExchangeInfo,
        binanceUsaExchangeInfo: ExchangeInfo,
        bittrexExchangeInfo: ExchangeInfo
    ) {
        binanceExchangeInfo.coins.forEach { it.addMarketCapAndId(binanceService!!.exchangeVisitor, listing) }
        binanceUsaExchangeInfo.coins.forEach { it.addMarketCapAndId(binanceUsaService!!.exchangeVisitor, listing) }
        bittrexExchangeInfo.coins.forEach { it.addMarketCapAndId(binanceService!!.exchangeVisitor, listing) }
    }

    private fun String.getResourceAsText(): String {
        return object {}.javaClass.getResource(this).readText()
    }

    fun getListing() = listing

    fun getBinanceService() = binanceService!!

    fun getBinanceUsaService() = binanceUsaService!!

    fun getBittrexService() = bittrexService!!
}