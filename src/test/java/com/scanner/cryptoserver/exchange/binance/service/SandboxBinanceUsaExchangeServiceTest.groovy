package com.scanner.cryptoserver.exchange.binance.service

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
    def "test getTickerData"() {
        when:
          def tickerList = service.getTickerData(symbol, "12h", "3d")

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
          "BTCUSDT"  | false
          "junkCoin" | true
    }
}
