package com.scanner.cryptoserver.util

import com.scanner.cryptoserver.exchange.binance.dto.CoinTicker

/**
 * Relative Strength Index. Implemented up to this specification:
 * http://en.wikipedia.org/wiki/Relative_strength
 */
class Rsi {

    private fun calculate(tickers: List<CoinTicker>, periodLength: Int): Double {
        val period = if (periodLength > tickers.size) tickers.size else periodLength
        val lastBar = tickers.size - 1
        val firstBar = lastBar - period + 1
        var gains = 0.0
        var losses = 0.0

        for (bar in firstBar + 1..lastBar) {
            val change = tickers[bar].close - tickers[bar - 1].close
            gains += 0.0.coerceAtLeast(change)
            losses += 0.0.coerceAtLeast(-change)
        }
        val change = gains + losses
        return if (change == 0.0) 50.0 else 100.0 * gains / change
    }

    fun setRsi(tickers: List<CoinTicker>, periodLength: Int) {
        if (periodLength > tickers.size) {
            return
        }
        val period = periodLength - 1
        val lastBar = tickers.size - 1

        for ((start, bar) in (period..lastBar).withIndex()) {
            val end = if (start + period > tickers.size - 1) tickers.size - 1 else start + period
            val subList = tickers.subList(start, end)
            val rsi = calculate(subList, periodLength)
            tickers[bar].rsi = rsi
        }
    }
}