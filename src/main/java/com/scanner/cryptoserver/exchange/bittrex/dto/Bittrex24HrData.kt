package com.scanner.cryptoserver.exchange.bittrex.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr
import com.scanner.cryptoserver.util.dto.Symbol

@JsonIgnoreProperties(ignoreUnknown = true)
class Bittrex24HrData {
    //the symbol/currency, such as "BTC-USD"
    var symbol: String = ""
    var high: Double = 0.0
    var low: Double = 0.0
    var volume: Double = 0.0
    var quoteVolume: Double = 0.0
    var percentChange: Double = 0.0

    fun coinDataAdapter(): CoinDataFor24Hr {
        val coin = CoinDataFor24Hr()
        coin.lowPrice = low
        coin.highPrice = high
        coin.priceChangePercent = percentChange
        coin.quoteVolume = quoteVolume
        coin.symbol = symbol
        val values = symbol.split("-")
        coin.coin = values[0]
        coin.currency = values[1]
        coin.volume = volume
        //todo: updated time
        return coin
    }

    fun symbolAdapter(): Symbol {
        val sym = Symbol()
        val values = symbol.split("-")
        sym.baseAsset = values[0]
        sym.quoteAsset = values[1]
        sym.symbol = symbol
        return sym
    }
}