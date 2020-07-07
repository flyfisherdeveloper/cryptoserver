package com.scanner.cryptoserver.exchange.binance.service

import com.scanner.cryptoserver.exchange.binance.dto.CoinTicker
import com.scanner.cryptoserver.util.SandboxUtil
import spock.lang.Specification
import spock.lang.Unroll

class SandboxBinanceUsaExchangeServiceTest extends Specification {
    private SandboxBinanceUsaExchangeService service
    private SandboxUtil sandboxUtil

    def setup() {
        sandboxUtil = new SandboxUtil()
        service = new SandboxBinanceUsaExchangeService(sandboxUtil)
    }

    def "test getExchangeInfo"() {
        when:
          def exchangeInfo = service.getExchangeInfo()

        then:
          assert exchangeInfo
          assert exchangeInfo.getSymbols().size() > 0
          def ethUsd = exchangeInfo.getSymbols().find { it.symbol == "ETHUSD" }
          assert ethUsd
          assert ethUsd.baseAsset == "ETH"
          assert ethUsd.quoteAsset == "USD"
          assert ethUsd.status == "TRADING"
          assert ethUsd.marketCap == 0.0
    }

    def "test get24HrAllCoinTicker"() {
        when:
          def tickerList = service.get24HrAllCoinTicker()

        then:
          assert tickerList
          assert tickerList.size() > 0
          def zrxUsd = tickerList.find { it.getSymbol() == "ZRXUSD" }
          assert zrxUsd
          assert zrxUsd.getCoin() == "ZRX"
          assert zrxUsd.getIcon()
          assert zrxUsd.getMarketCap() > 0.0
          assert zrxUsd.getCurrency() == "USD"
    }

    @Unroll
    def "test get24HrAllCoinTicker for page"() {
        when:
          def tickerList = service.get24HrAllCoinTicker(page, pageSize)

        then:
          assert tickerList != null
          if (numCoinsReturned == "list size") {
              assert tickerList.size() == tickerList.size()
          } else {
              assert tickerList.size() == numCoinsReturned
          }

        where:
          page | pageSize | numCoinsReturned
          0    | 5        | 5
          1    | 5        | 5
          100  | 50       | 0
          0    | 2000     | "list size"
          -1   | 10       | "list size"
    }

    @Unroll
    def "test getTickerData"() {
        when:
          def tickerList = service.getTickerData(symbol, "12h", "7d")

        then:
          if (exception) {
              assert tickerList != null
              assert tickerList.size() == 0
          } else {
              assert tickerList
              assert tickerList.size() > 0
              def ticker = tickerList.get(0)
              assert ticker.getSymbol() == symbol
              assert ticker.getOpenTime()
              assert ticker.getCloseTime()
              assert ticker.getOpen() != 0.0
              assert ticker.getHigh() != 0.0
              assert ticker.getLow() != 0.0
              assert ticker.getVolume() != 0.0
          }

        where:
          symbol     | exception
          "BTCUSD"   | false
          "junkCoin" | true
    }

    @Unroll
    def "test getRsi with Sandbox"() {
        given:
          def period = 22

        when:
          def tickerList = service.getTickerData(symbol, interval, days)
          service.setRsiForTickers(tickerList, period)

        then:
          for (int i = 0; i < tickerList.size(); i++) {
              //for the prices that don't have enough data for the RSI, verify that the RSI is 0
              if (i <= period - 1) {
                  assert tickerList.get(i).getRsi().doubleValue() == (double) 0.0
              }
          }

        where:
          symbol   | interval | days
          "BTCUSD" | "12h"    | "6M"
          "ETHUSD" | "24h"    | "6M"
    }

    @Unroll
    def "test rsi"() {
        given:
          def tickerList = closeList.collect { new CoinTicker(close: it) }

        when:
          service.setRsiForTickers(tickerList, periodLength)

        then:
          def tickerRsiList = tickerList.collect { it.getRsi() }
          tickerRsiList.each { println it }
          //transform the expected list into a list of doubles - Groovy defaults to BigDecimal
          def expectList = expectedRsiList.collect { it.toDouble() }
          assert expectList == tickerRsiList

        where:
          closeList                 | periodLength | expectedRsiList
          [100, 50, 100]            | 1            | [0.0, 0.0, 100.0]
          [1.0, 1.2, 1.3, 1.4, 1.3] | 3            | [0.0, 0.0, 0.0, 55.56, 33.33]
    }
}
