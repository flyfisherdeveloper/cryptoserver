package com.scanner.cryptoserver.exchange.bittrex.service

import com.scanner.cryptoserver.exchange.coinmarketcap.CoinMarketCapService
import com.scanner.cryptoserver.util.CacheUtil
import com.scanner.cryptoserver.util.UrlReader
import spock.lang.Specification

import java.util.function.Supplier


class BittrexServiceImplTest extends Specification {
    private UrlReader urlReader
    private CacheUtil cacheUtil
    private CoinMarketCapService coinMarketCapService
    private BittrexServiceImpl service

    def setup() {
        urlReader = Mock(UrlReader)
        cacheUtil = Mock(CacheUtil)
        coinMarketCapService = Mock(CoinMarketCapService)
        service = new BittrexServiceImpl(cacheUtil, coinMarketCapService, urlReader)
    }

    def "test getExchangeInfo"() {
        when:
          urlReader.readFromUrl() >> getMarketsJson()
          //here, we mock the cache util, and use the supplier passed in as an argument
          cacheUtil.retrieveFromCache(*_) >> { args ->
              Supplier supplier = args.get(2)
              return supplier.get()
          }

        then:
          def markets = service.getCoinDataFor24Hour()

        expect:
          assert markets
          def btc = markets.find { it.symbol == "BTC-USD" }
          assert btc
          //getting the value from the json
          assert btc.volume == 1120.23
    }

    def "test get24HrAllCoinTicker"() {
        when:
          urlReader.readFromUrl() >>> [getMarketsJson(), getTickersJson()]
          //here, we mock the cache util, and use the supplier passed in as an argument
          cacheUtil.retrieveFromCache(*_) >> { args ->
              Supplier supplier = args.get(2)
              return supplier.get()
          }

        then:
          def coins = service.get24HrAllCoinTicker()

        expect:
          assert coins
          def btc = coins.find { it.symbol == "BTC-USD" }
          assert btc
          //getting the value from the json - "lastTradeRate" is converted to "lastPrice"
          assert btc.lastPrice == 9834.33

          //note: the full trade link is null in the service for a unit test, but the service adds the symbol
          //to the end of the trade link with the currency-coin pair
          //just test to ensure that the pair is added to the end of the trade link
          assert btc.tradeLink.endsWith("USD-BTC")
    }

    def getMarketsJson() {
        return "[{\"symbol\":\"BTC-USD\",\"high\":\"9543.23\",\"low\":\"9523.89\",\"volume\":\"1120.23\",\"quoteVolume\":\"193023.56\",\"updatedAt\":\"2020-07-02T21:05:17.837Z\"}]"
    }

    def getTickersJson() {
        return "[{\"symbol\":\"BTC-USD\",\"lastTradeRate\":\"9834.33\",\"bidRate\":\"9831.23\",\"askRate\":\"9833.89\"}]"
    }
}