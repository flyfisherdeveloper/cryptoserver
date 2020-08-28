package com.scanner.cryptoserver.exchange.coinmarketcap

import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapData
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapListing
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.ExchangeInfo
import com.scanner.cryptoserver.util.CacheUtil
import com.scanner.cryptoserver.util.dto.Symbol
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
          def data = [:] as HashMap<String, CoinMarketCapData>
          data.put(baseAsset1, new CoinMarketCapData(symbol: baseAsset1, id: id1, name: name1))
          data.put(baseAsset2, new CoinMarketCapData(symbol: baseAsset2, id: id2, name: name2))
          def listing = new CoinMarketCapListing()
          listing.setData(data)

        when:
          cacheUtil.getExchangeNames() >> exchangeNameList
          cacheUtil.retrieveExchangeInfoFromCache("ExchangeInfo", _) >> exchangeInfo
          cacheUtil.retrieveFromCache("CoinMarketCap", _, _) >> listing

        then:
          def idSet = service.getIdSet()

        expect:
          assert idSet
          assert idSet.size() == listing.getData().size()
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
          if (badData) {
              //make sure we handle bad data effectively, without exceptions
              assert listing.getData() == null
          } else {
              assert listing.getData().size() == 1
              def btc = listing.getData().get(1)
              assert btc.getSymbol() == "BTC"
              assert btc.getName() == "Bitcoin"
              assert btc.getMarketCap() == 10000.35;
              assert btc.getVolume24HrUsd() == 50000.7804
          }

        where:
          json       | badData
          getJson()  | false
          "bad json" | true
    }

    def "test setMarketCapDataFor24HrData"() {
        given:
          def exchangeNameList = ["binance", "binanceUsa"]
          def exchangeInfo = new ExchangeInfo(symbols: [new Symbol(baseAsset: "BTC"), new Symbol(baseAsset: "ETH")])

          def listing = new CoinMarketCapListing()
          def btcCap = 121000000
          def btcAllCap = 500000000
          def data1 = new CoinMarketCapData(name: "BTC", marketCap: btcCap, symbol: "BTCUSD", id: 1, volume24HrUsd: btcAllCap)

          def ethCap = 22000000
          def ethAllCap = 4000000.8923
          //the service will format the numbers to two decimal places - test this
          def ethAllCapFormatted = 4000000.89
          def data2 = new CoinMarketCapData(name: "ETH", marketCap: ethCap, symbol: "ETHUSD", id: 2, volume24HrUsd: ethAllCap)
          def data = [:] as Map<String, CoinMarketCapData>
          data.put(data1.getName(), data1)
          data.put(data2.getName(), data2)
          listing.setData(data)

          def coin1 = new CoinDataFor24Hr(coin: "BTC", symbol: "BTCUSD")
          def coin2 = new CoinDataFor24Hr(coin: "ETH", symbol: "ETHUSD")
          def coinList = [coin1, coin2]

        when:
          cacheUtil.getExchangeNames() >> exchangeNameList
          cacheUtil.retrieveFromCache("ExchangeInfo", _, _) >> exchangeInfo
          cacheUtil.retrieveFromCache("CoinMarketCap", _, _) >> listing

        then:
          service.setMarketCapDataFor24HrData(coinList)

        expect:
          def btcCoin = coinList.find { it -> it.getCoin() == "BTC" }
          assert btcCoin.getMarketCap() == btcCap
          assert btcCoin.getVolume24HrUsd() == btcAllCap

          def ethCoin = coinList.find { it -> it.getCoin() == "ETH" }
          assert ethCoin.getMarketCap() == ethCap
          //make sure the number is formatted to two decimal places
          assert ethCoin.getVolume24HrUsd() == ethAllCapFormatted
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
                "\"market_cap\": 10000.35,\n" +
                "\"volume_24h\": 50000.7804\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "}"
    }
}