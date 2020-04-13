package com.scanner.cryptoserver.exchange.coinmarketcap

import com.scanner.cryptoserver.exchange.binance.dto.ExchangeInfo
import com.scanner.cryptoserver.exchange.binance.dto.Symbol
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapData
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapMap
import com.scanner.cryptoserver.util.CacheUtil
import com.scanner.cryptoserver.util.CoinMarketCapApiService
import spock.lang.Specification

class CoinMarketCapServiceTest extends Specification {
    private CoinMarketCapService service
    private CoinMarketCapApiService apiService
    private CacheUtil cacheUtil

    def setup() {
        cacheUtil = Mock(CacheUtil)
        apiService = Mock(CoinMarketCapApiService)
        service = new CoinMarketCapService(apiService, cacheUtil)
    }

    def "test getIdSet"() {
        given:
          def exchangeNameList = ["binance", "binanceUsa"]
          def baseAsset1 = "BTC"
          def baseAsset2 = "ETH"
          def id1 = 1
          def id2 = 2

          def exchangeInfo = new ExchangeInfo(symbols: [new Symbol(baseAsset: baseAsset1), new Symbol(baseAsset: baseAsset2)])
          def map = new CoinMarketCapMap(data: [new CoinMarketCapData(symbol: baseAsset1, id: id1), new CoinMarketCapData(symbol: baseAsset2, id: id2)])

        when:
          cacheUtil.getExchangeNames() >> exchangeNameList
          cacheUtil.retrieveFromCache(*_) >> exchangeInfo
          apiService.getCoinMarketCapMap() >> map

        then:
          def idSet = service.getIdSet()

        expect:
          assert idSet
          assert idSet.size() == map.getData().size()
          //"it" is a Groovy keyword: it is the name of the function parameter
          assert idSet.find { it == id1 } == 1
          assert idSet.find { it == id2 } == 2
    }

    def "test getCoinMarketCapListing"() {
        given:
          def idSet = [1].toSet()
          def json =
                  "{\n" +
                          "\"data\": {\n" +
                          "\"1\": {\n" +
                          "\"id\": 1,\n" +
                          "\"name\": \"Bitcoin\",\n" +
                          "\"symbol\": \"BTC\",\n" +
                          "\"quote\": {\n" +
                          "\"USD\": {\n" +
                          "\"market_cap\": 113563929433.21645\n" +
                          "}\n" +
                          "}\n" +
                          "}\n" +
                          "}\n" +
                          "}"

        when:
          //Here, the call to the api service is being mocked and returns mock json.
          //To make the supplier call that the api makes, we retrieve the arguments
          //to the mocked method call, and call the supplier so that the rest of the test will run.
          cacheUtil.retrieveFromCache(*_) >> { args ->
              def results = args.get(2).get()
              return results
          }
          apiService.makeExchangeQuotesApiCall(_) >> json

        then:
          def listing = service.getCoinMarketCapListing(idSet)

        expect:
          listing != null
          assert listing.getData() != null
          assert listing.getData().size() == 1
          assert listing.getData().get(0) != null
          assert listing.getData().get(0).getId() == 1
          assert listing.getData().get(0).getSymbol() == "BTC"
          assert listing.getData().get(0).getName() == "Bitcoin"
    }
}