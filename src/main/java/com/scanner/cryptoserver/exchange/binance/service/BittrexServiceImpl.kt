package com.scanner.cryptoserver.exchange.binance.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.scanner.cryptoserver.exchange.binance.dto.Bittrex24HrData
import com.scanner.cryptoserver.util.CacheUtil
import org.springframework.stereotype.Service
import java.net.URL

@Service
class BittrexServiceImpl(private val cacheUtil: CacheUtil) {
    private val exchangeName = "bittrex"
    private val all24HrTicker = "All24HourTicker"
    private val allTickers = "AllTickers"
    private val marketSummariesUrl = "https://api.bittrex.com/v3/markets/summaries"

    fun get24HrAllCoinTicker(): List<Bittrex24HrData?>? {
        return getMarkets()
    }

    private fun getMarkets(): List<Bittrex24HrData> {
        val url = URL(marketSummariesUrl)
        val mapper = jacksonObjectMapper()
        return mapper.readValue(url)
    }
}