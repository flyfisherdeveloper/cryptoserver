package com.scanner.cryptoserver.exchange.bittrex.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.scanner.cryptoserver.exchange.binance.dto.Bittrex24HrData
import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr
import com.scanner.cryptoserver.exchange.coinmarketcap.CoinMarketCapService
import com.scanner.cryptoserver.util.CacheUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URL

@Service
class BittrexServiceImpl(private val cacheUtil: CacheUtil, private val coinMarketCapService: CoinMarketCapService) {
    private val exchangeName = "bittrex"
    private val all24HrTicker = "All24HourTicker"
    private val allTickers = "AllTickers"

    @Value("\${exchanges.bittrex.market}")
    private val marketSummariesUrl: String? = null

    fun get24HrAllCoinTicker(): List<CoinDataFor24Hr> {
        val coins = getExchangeInfo()
        coinMarketCapService.setMarketCapFor24HrData(coins)
        coins.forEach { it.icon = cacheUtil.getIconBytes(it.coin) }
        return coins
    }

    fun getExchangeInfo(): List<CoinDataFor24Hr> {
        val markets = getMarkets()
        return markets.map { it.coinDataAdapter() }
    }

    private fun getMarkets(): List<Bittrex24HrData> {
        val results = URL(marketSummariesUrl).readText()
        val mapper = jacksonObjectMapper()
        return mapper.readValue(results)
    }
}