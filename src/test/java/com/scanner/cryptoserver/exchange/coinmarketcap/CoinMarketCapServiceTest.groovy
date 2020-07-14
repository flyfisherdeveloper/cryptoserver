package com.scanner.cryptoserver.exchange.coinmarketcap

import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr
import com.scanner.cryptoserver.exchange.binance.dto.ExchangeInfo
import com.scanner.cryptoserver.exchange.binance.dto.Symbol
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapData
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapMap
import com.scanner.cryptoserver.util.CacheUtil
import spock.lang.Specification
import spock.lang.Unroll

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
          def name1 = "Bitcoin"
          def baseAsset2 = "ETH"
          def name2 = "Ether"
          def id1 = 1
          def id2 = 2

          def exchangeInfo = new ExchangeInfo(symbols: [new Symbol(baseAsset: baseAsset1), new Symbol(baseAsset: baseAsset2)])
          def map = new CoinMarketCapMap(data: [new CoinMarketCapData(symbol: baseAsset1, id: id1, name: name1), new CoinMarketCapData(symbol: baseAsset2, id: id2, name: name2)])

        when:
          cacheUtil.getExchangeNames() >> exchangeNameList
          cacheUtil.retrieveFromCache("ExchangeInfo", _, _) >> exchangeInfo
          cacheUtil.retrieveFromCache("CoinMarketCap", _, _) >> map

        then:
          def idSet = service.getIdSet()

        expect:
          assert idSet
          assert idSet.size() == map.getData().size()
          //"it" is a Groovy keyword: it is the name of the function parameter
          assert idSet.find { it == id1 } == 1
          assert idSet.find { it == id2 } == 2
    }

    @Unroll
    def "test getCoinMarketCapListing"() {
        given:
          def idSet = [1].toSet()

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
          if (badData) {
              //make sure we handle bad data effectively, without exceptions
              assert listing.getData().size() == 0
          } else {
              assert listing.getData().size() == 1
              assert listing.getData().get(0) != null
              assert listing.getData().get(0).getId() == 1
              assert listing.getData().get(0).getSymbol() == "BTC"
              assert listing.getData().get(0).getName() == "Bitcoin"
              assert listing.getData().get(0).getMarketCap() == 10000000.35;
          }

        where:
          json       | badData
          getJson()  | false
          "bad json" | true
    }

    @Unroll
    def "test getCoinMarketCapInfo"() {
        given:
          def idSet = [1].toSet()

        when:
          apiService.makeExchangeInfoApiCall(_) >> json

        then:
          def listing = service.getCoinMarketCapInfo(idSet)

        expect:
          assert listing != null
          assert listing.getData() != null
          if (badData) {
              //make sure we handle bad data effectively, without exceptions
              assert listing.getData().size() == 0
          } else {
              assert listing.getData().size() == 1
              assert listing.getData().get(0) != null
              assert listing.getData().get(0).getId() == 1
              assert listing.getData().get(0).getSymbol() == "BTC"
              assert listing.getData().get(0).getName() == "Bitcoin"
              //the market cap is null since the endpoint being tested does not retrieve it
              assert listing.getData().get(0).getMarketCap() == null
          }

        where:
          json       | badData
          getJson()  | false
          "bad json" | true

    }

    def "test setMarketCapFor24HrData"() {
        given:
          def exchangeNameList = ["binance", "binanceUsa"]
          def exchangeInfo = new ExchangeInfo(symbols: [new Symbol(baseAsset: "BTC"), new Symbol(baseAsset: "ETH")])

          def map = new CoinMarketCapMap()
          def btcCap = 121000000
          def data1 = new CoinMarketCapData(name: "BTC", marketCap: btcCap, symbol: "BTCUSD", id: 1)

          def ethCap = 22000000
          def data2 = new CoinMarketCapData(name: "ETH", marketCap: ethCap, symbol: "ETHUSD", id: 2)
          map.setData([data1, data2])

          def coin1 = new CoinDataFor24Hr(coin: "BTC", symbol: "BTCUSD")
          def coin2 = new CoinDataFor24Hr(coin: "ETH", symbol: "ETHUSD")
          def data = [coin1, coin2]

        when:
          cacheUtil.getExchangeNames() >> exchangeNameList
          cacheUtil.retrieveFromCache("ExchangeInfo", _, _) >> exchangeInfo
          cacheUtil.retrieveFromCache("CoinMarketCap", _, _) >> map

        then:
          service.setMarketCapFor24HrData(data)

        expect:
          assert data.find { it.getCoin() == "BTC" }.getMarketCap() == btcCap
          assert data.find { it.getCoin() == "ETH" }.getMarketCap() == ethCap
    }

    def getJson() {
        return "{\n" +
                "\"data\": {\n" +
                "\"1\": {\n" +
                "\"id\": 1,\n" +
                "\"name\": \"Bitcoin\",\n" +
                "\"symbol\": \"BTC\",\n" +
                "\"description\": \"BTC is a digital coin.\",\n" +
                "\"logo\": \"http://mockPathToLogo.com\",\n" +
                "\"quote\": {\n" +
                "\"USD\": {\n" +
                "\"market_cap\": 10000000.35\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "}"
    }
}