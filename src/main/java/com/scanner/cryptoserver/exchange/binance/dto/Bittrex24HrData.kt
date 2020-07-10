package com.scanner.cryptoserver.exchange.binance.dto

data class Bittrex24HrData(val symbol: String, val high: Double, val low: Double, val volume: Double,
                           val quoteVolume: Double, val percentChange: Double, val updatedAt: String) {

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