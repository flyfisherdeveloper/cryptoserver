package com.scanner.cryptoserver.util

import com.scanner.cryptoserver.exchange.binance.dto.CoinTicker
import java.util.*

/**
 * Class used to calculate the relative strength for a list of tickers.
 */
class RsiCalc(private val periodLength: Int) {
    private val avgList: Stack<Averages> = Stack()

    private fun calculate(prices: List<CoinTicker>): Double {
        var value: Double
        val pricesSize = prices.size
        val lastPrice = pricesSize - 1
        val firstPrice = lastPrice - periodLength + 1
        var gains: Double
        var losses: Double
        var avgUp: Double
        var avgDown: Double
        val delta = prices[lastPrice].close - prices[lastPrice - 1].close

        gains = Math.max(0.0, delta)
        losses = Math.max(0.0, -delta)
        if (avgList.isEmpty()) {
            for (bar in firstPrice + 1..lastPrice) {
                val change = (prices[bar].close - prices[bar - 1].close)
                gains += Math.max(0.0, change)
                losses += Math.max(0.0, -change)
            }
            avgUp = gains / periodLength
            avgDown = losses / periodLength
            avgList.push(Averages(avgUp, avgDown))
        } else {
            val avg = avgList.pop()
            avgUp = avg.avgUp
            avgDown = avg.avgDown
            avgUp = (avgUp * (periodLength - 1) + gains) / periodLength
            avgDown = ((avgDown * (periodLength - 1) + losses)
                    / periodLength)
            avgList.add(Averages(avgUp, avgDown))
        }
        value = 100.0 - 100.0 / (1.0 + avgUp / avgDown)
        value = "%.2f".format(value).toDouble()
        return value
    }

    fun calculateRsiForTickers(prices: List<CoinTicker>, periodLength: Int) {
        var index = prices.size - 1
        while (index - periodLength >= 0) {
            val subList = prices.subList(0, index + 1)
            val rsi = calculate(subList)
            prices[index].rsi = rsi
            index--
        }
    }

    private class Averages(val avgUp: Double, val avgDown: Double)
}