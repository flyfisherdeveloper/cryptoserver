package com.scanner.cryptoserver.exchange.binance

import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr
import com.scanner.cryptoserver.exchange.binance.dto.CoinTicker
import com.scanner.cryptoserver.exchange.binance.dto.ExchangeInfo
import com.scanner.cryptoserver.exchange.binance.dto.Symbol
import com.scanner.cryptoserver.exchange.binance.service.BinanceExchangeService
import com.scanner.cryptoserver.exchange.binance.service.BinanceUrlExtractor
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapData
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapMap
import com.scanner.cryptoserver.util.CacheUtil
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate
import spock.lang.Specification
import spock.lang.Unroll

import java.time.LocalDateTime
import java.time.ZoneOffset

class BinanceExchangeServiceTest extends Specification {
    private BinanceExchangeService service
    private RestOperations restTemplate
    private BinanceUrlExtractor urlExtractor
    private CacheUtil cacheUtil

    def setup() {
        restTemplate = Mock(RestTemplate)
        urlExtractor = Mock(BinanceUrlExtractor)
        cacheUtil = Mock(CacheUtil)
        service = new BinanceExchangeService(restTemplate, urlExtractor, cacheUtil)
    }

    def "test get24HrAllCoinTicker() when cache has data"() {
        given:
          def coinData = new CoinDataFor24Hr()
          def symbol = "BTCUSD"
          coinData.setSymbol(symbol)

        when:
          cacheUtil.retrieveFromCache(*_) >> [coinData]

        then:
          def coins = service.get24HrAllCoinTicker()

        expect:
          assert coins
          assert coins.size() == 1
          assert coins.get(0).getSymbol() == symbol
    }

    @Unroll
    def "test getMarkets() returns expected markets"() {
        given:
          //note that the market cap is not being set in the symbols here, but is being verified in the "then" section:
          //the service will set the symbol market cap based on what is returned in the coinMarketCapMap object in the "when" section
          def symbol1 = new Symbol(baseAsset: coin1, quoteAsset: market1)
          def symbol2 = new Symbol(baseAsset: coin2, quoteAsset: market2)
          def exchangeInfo = new ExchangeInfo()
          exchangeInfo.setSymbols([symbol1, symbol2])

          def coinMarketCapMap = new CoinMarketCapMap()
          def data1 = new CoinMarketCapData(symbol: coin1, marketCap: marketCap1)
          def data2 = new CoinMarketCapData(symbol: coin2, marketCap: marketCap2)
          coinMarketCapMap.setData([data1, data2])

        when: "mocks are called"
          cacheUtil.retrieveFromCache(*_) >>> [exchangeInfo, coinMarketCapMap]

        then:
          def markets = service.getMarkets()

        expect:
          assert markets
          assert markets == expectedMarkets.toSet()
          //verify that the market caps were set in the exchange symbols
          assert exchangeInfo.getSymbols().get(0).getMarketCap() == marketCap1
          assert exchangeInfo.getSymbols().get(1).getMarketCap() == marketCap2

        where:
          coin1 | market1 | coin2 | market2 | marketCap1  | marketCap2 | expectedMarkets
          "BTC" | "USD"   | "LTC" | "USDC"  | 17000000000 | 2000000    | ["USD", "USDC"]
          "BTC" | "USD"   | "LTC" | "USD"   | 18000000000 | 30000000   | ["USD"]
    }

    //Here, we do a unit test of the 24Hour price ticker instead of an integration test
    // since an integration test would call the api and use up quota.
    @Unroll("Test that #symbol price info is correct for 24 Hour Coin Ticker service")
    def "test get24HrAllCoinTicker"() {
        given:
          //the following represents 24-hour data for a coin
          def map = new LinkedHashMap()
          map["symbol"] = symbol
          map["priceChange"] = priceChange
          map["priceChangePercent"] = priceChangePercent
          map["lastPrice"] = lastPrice
          map["highPrice"] = highPrice
          map["lowPrice"] = lowPrice
          map["volume"] = volume
          map["quoteVolume"] = quoteVolume

          def coinList = (symbol != null) ? [map] as LinkedHashMap<String, String>[] : [] as LinkedHashMap<String, String>[]
          def linkedHashMapResponse = ResponseEntity.of(Optional.of(coinList)) as ResponseEntity<LinkedHashMap[]>

          //the following represents exchange information - metadata about coins on an exchange
          def exchangeInfo = new ExchangeInfo()
          def symbol1 = new Symbol(symbol: symbol, quoteAsset: "BTC", status: status)
          def symbol2 = new Symbol(symbol: symbol, quoteAsset: "USD", status: status)
          def symbol3 = new Symbol(symbol: symbol, quoteAsset: "USDT", status: status)
          def symbol4 = new Symbol(symbol: symbol, quoteAsset: "BUSD", status: status)
          def symbols = [symbol1, symbol2, symbol3, symbol4]
          exchangeInfo.setSymbols(symbols)
          def exchangeInfoResponse = ResponseEntity.of(Optional.of(exchangeInfo)) as ResponseEntity<ExchangeInfo>

          //the following represents coin market cap data for certain coins
          def coinMarketCapMap = new CoinMarketCapMap()
          def data1 = new CoinMarketCapData(symbol: coin, marketCap: marketCap)
          def data2 = new CoinMarketCapData(symbol: coin, marketCap: marketCap)
          def data3 = new CoinMarketCapData(symbol: coin, marketCap: marketCap)
          def data4 = new CoinMarketCapData(symbol: coin, marketCap: marketCap)
          coinMarketCapMap.setData([data1, data2, data3, data4])

        when: "mocks are called"
          cacheUtil.retrieveFromCache(_, "binance-ExchangeInfo", _) >>> [exchangeInfo, exchangeInfo]
          cacheUtil.retrieveFromCache(_, "MarketCap", _) >>> [coinMarketCapMap]
          restTemplate.getForEntity(*_,) >>> [linkedHashMapResponse, exchangeInfoResponse]

        then: "the service is called"
          def allCoins = service.call24HrAllCoinTicker()

        expect:
          assert allCoins != null
          if (isCoinValid) {
              assert allCoins.size() == coinList.size()
              assert allCoins[0].symbol == symbol
              assert allCoins[0].coin == coin
              assert allCoins[0].currency == currency
              assert allCoins[0].priceChange == priceChange as Double
              assert allCoins[0].priceChangePercent == priceChangePercent as Double
              assert allCoins[0].lastPrice == lastPrice as Double
              assert allCoins[0].highPrice == highPrice as Double
              assert allCoins[0].lowPrice == lowPrice as Double
              assert allCoins[0].volume == volume as Double
              assert allCoins[0].quoteVolume == quoteVolume as Double
              assert allCoins[0].tradeLink
              assert allCoins[0].marketCap == marketCap
          } else {
              assert allCoins.isEmpty()
          }

        where:
          symbol    | coin  | currency | status    | priceChange | priceChangePercent | lastPrice | highPrice | lowPrice  | volume        | quoteVolume   | marketCap | isCoinValid
          "LTCUSD"  | "LTC" | "USD"    | "TRADING" | "13.2"      | "1.2"              | "14.3"    | "16.3"    | "11.17"   | "23987.23"    | "54.23"       | 20000.0   | true
          "BTCBUSD" | "BTC" | "BUSD"   | "TRADING" | "439.18"    | "4.32"             | "8734.56" | "8902.87" | "8651.23" | "88922330.09" | "10180.18"    | 50000.0   | true
          "BTCUSDT" | "BTC" | "USDT"   | "TRADING" | "439.18"    | "4.32"             | "8734.56" | "8902.87" | "8651.23" | "88922330.09" | "10180.18"    | 50000.0   | true
          //verify that coins that are not trading (status is "BREAK") do not get returned from the service
          "BTCUSDT" | "BTC" | "USDT"   | "BREAK"   | "439.18"    | "4.32"             | "8734.56" | "8902.87" | "8651.23" | "88922330.09" | "10180.18"    | 50000.0   | false
          //verify that non-USA currencies (EUR) do not get returned from the service
          "BTCEUR"  | "BTC" | "EUR"    | "TRADING" | "439.18"    | "4.32"             | "8823.22" | "8734.56" | "8902.87" | "8651.23"     | "88922330.09" | 50000.0   | false
          //verify that leveraged tokens do not get returned from the service
          "XRPBEAR" | "XRP" | "USD"    | "TRADING" | "439.18"    | "4.32"             | "0.25"    | "0.28"    | "0.21"    | "8651.23"     | "88922330.09" | 10000.0   | false
          "XRPBULL" | "XRP" | "USD"    | "TRADING" | "439.18"    | "4.32"             | "0.25"    | "0.28"    | "0.21"    | "8651.23"     | "88922330.09" | 10000.0   | false
          //verify that the service handles the case of no data being returned
          null      | null  | null     | null      | null        | null               | null      | null      | null      | null          | null          | null      | false
    }

    @Unroll("Test that when volume changes from #volume1 to #volume2 that the volume percent change is #percentChange")
    def "test getPercentChange"() {
        when:
          def change = service.getPercentChange(volume1, volume2)

        then:
          assert change == percentChange

        where:
          volume1 | volume2 | percentChange
          10.0    | 20.0    | 100.0
          10.0    | 5.0     | -50.0
    }

    @Unroll("Test that when #symbol volume changes from #quoteVolumePrevious to #quoteVolueNew that the expected volume percent change is #expectedVolumePercentChange%")
    def "test add24HrVolumeChange"() {
        given:
          def now = LocalDateTime.now()
          def closeTime1 = now.minusDays(1).minusHours(1)

          def coin = new CoinDataFor24Hr()
          coin.setSymbol(symbol)
          def coinList = [coin]

          //The zeroes are just filler data - not needed for the test, but are necessary for the test to complete.
          def coinDataList1 = [0L, "0.0", "0.0", "0.0", "0.0", "0.0", closeTime1.toInstant(ZoneOffset.UTC).toEpochMilli(), quoteVolumePrevious.toString(), 0]
          def coinDataList2 = [0L, "0.0", "0.0", "0.0", "0.0", "0.0", now.toInstant(ZoneOffset.UTC).toEpochMilli(), quoteVolueNew.toString(), 0]

          def coinData = new Object[2]
          coinData[0] = coinDataList1
          coinData[1] = coinDataList2
          def response = ResponseEntity.of(Optional.of(coinData)) as ResponseEntity<Object[]>

        when: "mocks are called"
          restTemplate.getForEntity(*_) >> response
          response.getBody() >> coinData

        then: "the service is called"
          service.add24HrVolumeChange(coinList)

        expect:
          assert coinList[0].getVolumeChangePercent() == expectedVolumePercentChange

        where:
          symbol   | quoteVolueNew | quoteVolumePrevious | expectedVolumePercentChange
          "BTCUSD" | 20.0          | 10.0                | 100.0
          "LTCUSD" | 10.0          | 20.0                | -50.0
    }

    @Unroll("Test call of coin ticker for #symbol for #interval and #daysOrMonths")
    def "test callCoinTicker"() {
        given:
          def now = LocalDateTime.now()
          def closeTime1 = now.minusHours(2).toInstant(ZoneOffset.UTC).toEpochMilli()
          def closeTime2 = now.minusHours(1).toInstant(ZoneOffset.UTC).toEpochMilli()
          def response1 = getMockCoinTicker(closeTime1, closeTime2)

          def prevDayCloseTime1 = now.minusDays(1).minusHours(2).toInstant(ZoneOffset.UTC).toEpochMilli()
          def prevDayCloseTime2 = now.minusDays(1).minusHours(1).toInstant(ZoneOffset.UTC).toEpochMilli()
          def response2 = getMockCoinTicker(prevDayCloseTime1, prevDayCloseTime2)

          def caughtException

        when:
          //Here, response1 is the response for the first time the rest template is called; response2 is the second time the rest template is called.
          restTemplate.getForEntity(*_) >>> [response1, response2]

        then:
          def tickers = null
          //the service call can throw an exception if too much data is requested: account for this
          try {
              tickers = service.callCoinTicker(symbol, interval, daysOrMonths)
          } catch (Exception e) {
              caughtException = e
          }

        expect:
          if (exception.expected) {
              //in this case, too much data is requested: test the exception
              assert caughtException != null
              assert exception.type.isInstance(caughtException)
          } else {
              assert caughtException == null
              assert tickers
              assert tickers.size() > 0
              //"it" is a groovy keyword = it is the function parameter for each item in the list
              tickers.each { assert it.getSymbol() == symbol }
              //Here, we verify that the service for 1hr/1m makes two asynch calls to get data:
              //This is because the service needs to make two calls for 1-hour month data, since there is too much data to bring back in one call.
              //Here, we verify this by checking the size of the returned data: it should be 4 (meaning, both responses in the "when" section were used - two calls).
              //Otherwise, only two data should be returned.
              if (interval == "1h" && daysOrMonths == "1m") {
                  assert tickers.size() == 4
                  //also, verify that the tickers are sorted - the service sorts the tickers since the data may come back unpredictably due to asynch calls
                  assert prevDayCloseTime1 == tickers[0].getCloseTime()
                  assert prevDayCloseTime2 == tickers[1].getCloseTime()
                  assert closeTime1 == tickers[2].getCloseTime()
                  assert closeTime2 == tickers[3].getCloseTime()
              } else {
                  assert tickers.size() == 2
              }
          }

        where:
          symbol   | interval | daysOrMonths | exception
          "BTCUSD" | "1h"     | "1d"         | [expected: false]
          "BTCUSD" | "1h"     | "1m"         | [expected: false]
          "BTCUSD" | "4h"     | "1m"         | [expected: false]
          "BTCUSD" | "1h"     | "12m"        | [expected: true, type: RuntimeException]
    }

    @Unroll("Test call of get ticker data with coin cashe exists: #coinCacheExists and coin in cache: #inCache")
    def "test getTickerData"() {
        given:
          def coinTicker1 = new CoinTicker(symbol: symbol, openDate: "Nov 10, 2019", closeDate: "Nov 11, 2019")
          def coinTicker2 = new CoinTicker(symbol: symbol, openDate: "Nov 12, 2019", closeDate: "Nov 13, 2019")
          def coinTickerList = [coinTicker1, coinTicker2]

        when:
          if (inCache) {
              cacheUtil.retrieveFromCache(*_) >> coinTickerList
          } else {
              cacheUtil.retrieveFromCache(*_) >> null
          }

        then:
          def coins = service.getTickerData(symbol, interval, daysOrMonths)

        expect:
          assert coins != null
          if (inCache) {
              //if we pass this test, then we ensure that the coin was retrieved from the cache
              assert coins.size() == coinTickerList.size()
          } else {
              assert coins.size() == 0
          }

        where:
          symbol   | interval | daysOrMonths | inCache
          "BTCUSD" | "4h"     | "7d"         | true
          "BTCUSD" | "4h"     | "7d"         | false
          "BTCUSD" | "1h"     | "1d"         | false
    }

    ResponseEntity<Object[]> getMockCoinTicker() {
        def now = LocalDateTime.now()
        def closeTime1 = now.minusDays(1).minusHours(1).toInstant(ZoneOffset.UTC).toEpochMilli()
        def closeTime2 = now.toInstant(ZoneOffset.UTC).toEpochMilli()
        return getMockCoinTicker(closeTime1, closeTime2)
    }

    ResponseEntity<Object[]> getMockCoinTicker(long closeTime1, long closeTime2) {
        //The zeroes are just filler data - not needed for the tests, but are necessary for the tests to complete.
        def coinDataList1 = [0L, "0.0", "0.0", "0.0", "0.0", "0.0", closeTime1, "0.0", 0]
        def coinDataList2 = [0L, "0.0", "0.0", "0.0", "0.0", "0.0", closeTime2, "0.0", 0]

        def coinData = new Object[2]
        coinData[0] = coinDataList1
        coinData[1] = coinDataList2
        def response = ResponseEntity.of(Optional.of(coinData)) as ResponseEntity<Object[]>
        return response
    }
}