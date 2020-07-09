package com.scanner.cryptoserver.exchange.binance.dto

data class Bittrex24HrData(val symbol: String, val high: Double, val low: Double, val volume: Double,
                           val quoteVolume: Double, val percentChange: Double, val updatedAt: String)