package com.scanner.cryptoserver.exchange.bittrex.service

import com.scanner.cryptoserver.exchange.binance.dto.Bittrex24HrData
import spock.lang.Specification


class Bittrex24HrDataTest extends Specification {

    def "test coinDataAdapter"() {
        given:
          def symbol = "BTC-USD"
          def coin = "BTC"
          def currency = "USD"
          def high = 9800.24
          def low = 9700.24
          def percentChange = 0.10
          def quoteVolume = 150.10
          def volume = 1000000
          def bittrexCoin = new Bittrex24HrData()
          bittrexCoin.setSymbol(symbol)
          bittrexCoin.setHigh(high)
          bittrexCoin.setLow(low)
          bittrexCoin.setVolume(volume)
          bittrexCoin.setQuoteVolume(quoteVolume)
          bittrexCoin.setPercentChange(percentChange)

        when:
          def coin24Hr = bittrexCoin.coinDataAdapter()

        then:
          assert coin24Hr != null
          assert coin24Hr.getSymbol() == symbol
          assert coin24Hr.getCoin() == coin
          assert coin24Hr.getCurrency() == currency
          assert coin24Hr.getVolume() == volume
          assert coin24Hr.getLowPrice() == low
          assert coin24Hr.getHighPrice() == high
          assert coin24Hr.getPriceChangePercent() == percentChange
          assert coin24Hr.getQuoteVolume() == quoteVolume
    }
}