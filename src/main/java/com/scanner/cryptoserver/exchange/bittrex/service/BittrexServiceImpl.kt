package com.scanner.cryptoserver.exchange.bittrex.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr
import com.scanner.cryptoserver.exchange.binance.dto.CoinTicker
import com.scanner.cryptoserver.exchange.bittrex.dto.Bittrex24HrData
import com.scanner.cryptoserver.exchange.bittrex.dto.BittrexTicker
import com.scanner.cryptoserver.exchange.coinmarketcap.CoinMarketCapService
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.ExchangeInfo
import com.scanner.cryptoserver.exchange.service.ExchangeService
import com.scanner.cryptoserver.util.CacheUtil
import com.scanner.cryptoserver.util.UrlReader
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.function.Supplier

@Service(value = "bittrexService")
class BittrexServiceImpl(private val cacheUtil: CacheUtil, private val coinMarketCapService: CoinMarketCapService, private val urlReader: UrlReader) : ExchangeService {
    private val Log = LoggerFactory.getLogger(BittrexServiceImpl::class.java)
    private val nonUsaMarkets = listOf("EUR")
    private val EXCHANGE_NAME = "bittrex"
    private val ALL_24_HR_TICKER = "All24HourTicker"
    private val ALL_MARKET_TICKERS = "AllMarketTickers"
    private val ALL_TICKERS = "AllTickers"
    private val ALL_MARKETS = "AllMarkets"
    private val EXCHANGE_INFO = "ExchangeInfo"

    @Value("\${exchanges.bittrex.market}")
    private val marketSummariesUrl: String? = null

    @Value("\${exchanges.bittrex.trade}")
    private val tradeUrl: String? = null

    @Value("\${exchanges.bittrex.tickers}")
    private val tickersUrl: String? = null

    init {
        cacheUtil.addExchangeInfoSupplier(EXCHANGE_NAME, exchangeInfoSupplier)
    }

    override fun get24HrAllCoinTicker(): List<CoinDataFor24Hr> {
        var coins = getCoinDataFor24Hour()
        //we need to make another api call to get the "current price", which is "lastTradeRate" in the json
        val tickers = getTickersFromCache()
        coinMarketCapService.setMarketCapDataFor24HrData(coins)
        //exclude coins that don't have a market cap - they are probably old coins that the exchange doesn't support anymore
        coins = coins.filter { it.marketCap > 0.0 }
        coins.forEach {
            it.icon = cacheUtil.getIconBytes(it.coin)
            it.tradeLink = tradeUrl + it.currency + "-" + it.coin
            val bittrexTicker = tickers.find { ticker -> ticker.symbol == it.symbol }
            val lastTradeRate = bittrexTicker?.lastTradeRate
            it.lastPrice = lastTradeRate
        }
        return coins
    }

    override fun get24HrAllCoinTicker(page: Int, pageSize: Int): MutableList<CoinDataFor24Hr> {
        TODO("Not yet implemented")
    }

    fun getCoinDataFor24Hour(): List<CoinDataFor24Hr> {
        val cacheName = "$EXCHANGE_NAME-$ALL_24_HR_TICKER"
        val markets = cacheUtil.retrieveFromCache(cacheName, ALL_MARKET_TICKERS) { getMarkets() }
        //remove currency markets that are not USA-based, such as the Euro ("EUR")
        val coins = markets.map { it.coinDataAdapter() }
        return coins.filter { !nonUsaMarkets.contains(it.currency) }
    }

    private fun getMarkets(): List<Bittrex24HrData> {
        val results: String = if (marketSummariesUrl == null) {
            urlReader.readFromUrl()
        } else {
            Log.debug("Retrieving markets for Bittrex exchange: {}", marketSummariesUrl)
            urlReader.readFromUrl(marketSummariesUrl)
        }
        val mapper = jacksonObjectMapper()
        return mapper.readValue(results) as List<Bittrex24HrData>
    }

    override fun getTickerData(symbol: String?, interval: String?, daysOrMonths: String?): MutableList<CoinTicker> {
        TODO("Not yet implemented")
    }

    override fun setRsiForTickers(tickers: MutableList<CoinTicker>?, periodLength: Int) {
        TODO("Not yet implemented")
    }

    override fun get24HourCoinData(symbol: String?): CoinDataFor24Hr {
        return getCoinDataFor24Hour().find { it.symbol == symbol }!!
    }

    override fun getRsiTickerData(symbols: MutableList<String>?): MutableList<CoinTicker> {
        TODO("Not yet implemented")
    }

    override fun getExchangeInfoSupplier(): Supplier<ExchangeInfo> {
        return Supplier {
            return@Supplier exchangeInfo
        }
    }

    override fun getExchangeInfo(): ExchangeInfo {
        val cacheName = "$EXCHANGE_NAME-$ALL_MARKETS"
        //get the markets from the Bittrex API
        val markets = cacheUtil.retrieveFromCache(cacheName, ALL_MARKETS) { getMarkets() }
        //now adapt the Bittrex markets objects to Symbols
        val symbolList = markets.map { it.symbolAdapter() }
        val exchangeInfo = ExchangeInfo()
        //put the symbols in an exchange info object, to be consistent with all the exchange info from other exchanges
        exchangeInfo.symbols = symbolList
        //remove currency markets that are not USA-based, such as the Euro ("EUR")
        exchangeInfo.symbols.removeIf { nonUsaMarkets.contains(it.quoteAsset) }
        //put the Bittrex exchange info in the cache
        val name = "$EXCHANGE_NAME-$EXCHANGE_INFO"
        cacheUtil.putInCache(EXCHANGE_INFO, name, exchangeInfo)
        return exchangeInfo
    }

    override fun retrieveExchangeInfoFromCache(): ExchangeInfo {
        val name = "$EXCHANGE_NAME-$EXCHANGE_INFO"
        val exchangeInfo = cacheUtil.retrieveFromCache<ExchangeInfo>(EXCHANGE_INFO, name, null)
        return exchangeInfo
    }

    private fun getTickersFromCache(): List<BittrexTicker> {
        val cacheName = "$EXCHANGE_NAME-$ALL_24_HR_TICKER"
        val tickers = Supplier { getTickers() }
        return cacheUtil.retrieveFromCache<List<BittrexTicker>>(cacheName, ALL_TICKERS, tickers)
    }

    private fun getTickers(): List<BittrexTicker> {
        val results: String = if (tickersUrl == null) {
            urlReader.readFromUrl()
        } else {
            Log.debug("Retrieving tickers for Bittrex exchange: {}", tickersUrl)
            urlReader.readFromUrl(tickersUrl)
        }
        val mapper = jacksonObjectMapper()
        return mapper.readValue(results)
    }
}
