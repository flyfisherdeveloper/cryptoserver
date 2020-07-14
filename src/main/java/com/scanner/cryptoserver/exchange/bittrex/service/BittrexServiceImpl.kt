package com.scanner.cryptoserver.exchange.bittrex.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr
import com.scanner.cryptoserver.exchange.bittrex.dto.Bittrex24HrData
import com.scanner.cryptoserver.exchange.bittrex.dto.BittrexTicker
import com.scanner.cryptoserver.exchange.coinmarketcap.CoinMarketCapService
import com.scanner.cryptoserver.util.CacheUtil
import com.scanner.cryptoserver.util.UrlReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class BittrexServiceImpl(private val cacheUtil: CacheUtil, private val coinMarketCapService: CoinMarketCapService, private val urlReader: UrlReader) {
    private val exchangeName = "bittrex"
    private val all24HrTicker = "All24HourTicker"
    private val allTickers = "AllTickers"

    @Value("\${exchanges.bittrex.market}")
    private val marketSummariesUrl: String? = null

    @Value("\${exchanges.bittrex.trade}")
    private val tradeUrl: String? = null

    @Value("\${exchanges.bittrex.tickers}")
    private val tickersUrl: String? = null

    fun get24HrAllCoinTicker(): List<CoinDataFor24Hr> {
        val coins = getExchangeInfo()
        //we need to make another api call to get the "current price", which is "lastTradeRate" in the json
        val tickers = getTickers()
        coinMarketCapService.setMarketCapFor24HrData(coins)
        coins.forEach {
            it.icon = cacheUtil.getIconBytes(it.coin)
            it.tradeLink = tradeUrl + it.currency + "-" + it.coin
            val ticker = tickers.find { ticker -> ticker.symbol == it.symbol }
            it.lastPrice = ticker!!.lastTradeRate
        }
        return coins
    }

    fun getExchangeInfo(): List<CoinDataFor24Hr> {
        val markets = getMarkets()
        return markets.map { it.coinDataAdapter() }
    }

    private fun getMarkets(): List<Bittrex24HrData> {
        val results: String = if (marketSummariesUrl == null) {
            urlReader.readFromUrl()
        } else {
            urlReader.readFromUrl(marketSummariesUrl)
        }
        val mapper = jacksonObjectMapper()
        return mapper.readValue(results)
    }

    private fun getTickers(): List<BittrexTicker> {
        val results: String = if (tickersUrl == null) {
            urlReader.readFromUrl()
        } else {
            urlReader.readFromUrl(tickersUrl)
        }
        val mapper = jacksonObjectMapper()
        return mapper.readValue(results)
    }
}