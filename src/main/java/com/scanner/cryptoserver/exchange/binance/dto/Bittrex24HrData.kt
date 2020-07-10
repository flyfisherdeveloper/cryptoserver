package com.scanner.cryptoserver.exchange.binance.dto

class Bittrex24HrData {
    var symbol: String = ""
    var high: Double = 0.0
    var low: Double = 0.0
    var volume: Double = 0.0
    var quoteVolume: Double = 0.0
    var percentChange: Double = 0.0
    var updatedAt: String = ""

    public fun coinDataAdapter(): CoinDataFor24Hr {
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
}