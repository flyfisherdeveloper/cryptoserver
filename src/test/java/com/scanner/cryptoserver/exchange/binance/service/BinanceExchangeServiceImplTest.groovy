package com.scanner.cryptoserver.exchange.binance.service

import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr
import com.scanner.cryptoserver.exchange.binance.dto.CoinTicker
import com.scanner.cryptoserver.exchange.coinmarketcap.CoinMarketCapService
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapData
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapListing
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.ExchangeInfo
import com.scanner.cryptoserver.util.CacheUtil
import com.scanner.cryptoserver.util.dto.Symbol
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate
import spock.lang.Specification
import spock.lang.Unroll

import java.time.LocalDateTime
import java.time.ZoneOffset

class BinanceExchangeServiceImplTest extends Specification {
    private BinanceExchangeServiceImpl service
    private RestOperations restTemplate
    private BinanceUrlExtractor urlExtractor
    private CoinMarketCapService coinMarketCapService
    private CacheUtil cacheUtil

    def setup() {
        restTemplate = Mock(RestTemplate)
        urlExtractor = Mock(BinanceUrlExtractor)
        coinMarketCapService = Mock(CoinMarketCapService)
        cacheUtil = Mock(CacheUtil)
        service = new BinanceExchangeServiceImpl(restTemplate, urlExtractor, cacheUtil, coinMarketCapService)
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
          def symbol1 = new Symbol(baseAsset: coin1, quoteAsset: market1)
          def symbol2 = new Symbol(baseAsset: coin2, quoteAsset: market2)
          def exchangeInfo = new ExchangeInfo()
          exchangeInfo.setSymbols([symbol1, symbol2])

          def listing = new CoinMarketCapListing()
          def data1 = new CoinMarketCapData(symbol: coin1)
          def data2 = new CoinMarketCapData(symbol: coin2)
          def dataMap = [:] as HashMap<String, CoinMarketCapData>
          dataMap.put(coin1, data1)
          dataMap.put(coin2, data2)
          listing.setData(dataMap)

        when: "mocks are called"
          cacheUtil.retrieveFromCache(*_) >> exchangeInfo
          coinMarketCapService.getCoinMarketCapListing() >> listing

        then:
          def markets = service.getMarkets()

        expect:
          assert markets
          assert markets == expectedMarkets.toSet()

        where:
          coin1 | market1 | coin2 | market2 | expectedMarkets
          "BTC" | "USD"   | "LTC" | "USDC"  | ["USD", "USDC"]
          "BTC" | "USD"   | "LTC" | "USD"   | ["USD"]
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
          def exchangeSymbol = new Symbol(symbol: symbol, quoteAsset: currency, status: status)
          def exchangeSymbols = [exchangeSymbol]
          exchangeInfo.setSymbols(exchangeSymbols)
          def exchangeInfoResponse = ResponseEntity.of(Optional.of(exchangeInfo)) as ResponseEntity<ExchangeInfo>

          //the following represents coin market cap data for certain coins
          def coinMarketCapMap = new CoinMarketCapListing()
          def data = new CoinMarketCapData(symbol: coin, marketCap: marketCap)
          coinMarketCapMap.setData(coin: data)

        when: "mocks are called"
          cacheUtil.retrieveFromCache(_, "binance-ExchangeInfo", _) >>> [exchangeInfo, exchangeInfo]
          coinMarketCapService.getCoinMarketCapListing() >> coinMarketCapMap
          restTemplate.getForEntity(*_,) >>> [linkedHashMapResponse, exchangeInfoResponse]
          //here, we mock the call to the market cap service that sets the market cap
          //this ensures that the service makes the call to set the market cap
          coinMarketCapService.setMarketCapDataFor24HrData(*_) >> { args ->
              def list = args.get(1) as List<CoinDataFor24Hr>
              list.forEach { it.setMarketCap(marketCap) }
          }

        then: "the service is called"
          def allCoins = service.get24HrCoinData()

        expect:
          assert allCoins != null
          if (isCoinValid) {
              assert allCoins.size() == coinList.size()
              assert allCoins[0].getSymbol() == symbol
              assert allCoins[0].getCoin() == coin
              assert allCoins[0].getCurrency() == currency
              assert allCoins[0].getPriceChange() == priceChange as Double
              assert allCoins[0].getPriceChangePercent() == priceChangePercent as Double
              assert allCoins[0].getLastPrice() == lastPrice as Double
              assert allCoins[0].getHighPrice() == highPrice as Double
              assert allCoins[0].getLowPrice() == lowPrice as Double
              assert allCoins[0].getVolume() == volume as Double
              assert allCoins[0].getQuoteVolume() == quoteVolume as Double
              assert allCoins[0].getTradeLink()
              assert allCoins[0].getMarketCap() == marketCap
          } else {
              assert allCoins.isEmpty()
          }

        where:
          symbol        | coin      | currency | status    | priceChange | priceChangePercent | lastPrice | highPrice | lowPrice  | volume        | quoteVolume   | marketCap | isCoinValid
          "LTCUSD"      | "LTC"     | "USD"    | "TRADING" | "13.2"      | "1.2"              | "14.3"    | "16.3"    | "11.17"   | "23987.23"    | "54.23"       | 20000.0   | true
          "BTCBUSD"     | "BTC"     | "BUSD"   | "TRADING" | "439.18"    | "4.32"             | "8734.56" | "8902.87" | "8651.23" | "88922330.09" | "10180.18"    | 50000.0   | true
          "BTCUSDT"     | "BTC"     | "USDT"   | "TRADING" | "439.18"    | "4.32"             | "8734.56" | "8902.87" | "8651.23" | "88922330.09" | "10180.18"    | 50000.0   | true
          //verify that coins that are not trading (status is "BREAK") do not get returned from the service
          "BTCUSDT"     | "BTC"     | "USDT"   | "BREAK"   | "439.18"    | "4.32"             | "8734.56" | "8902.87" | "8651.23" | "88922330.09" | "10180.18"    | 50000.0   | false
          //verify that non-USA currencies (EUR) do not get returned from the service
          "BTCEUR"      | "BTC"     | "EUR"    | "TRADING" | "439.18"    | "4.32"             | "8823.22" | "8734.56" | "8902.87" | "8651.23"     | "88922330.09" | 50000.0   | false
          //verify that leveraged tokens do not get returned from the service
          "XRPBEAR"     | "XRP"     | "USD"    | "TRADING" | "439.18"    | "4.32"             | "0.25"    | "0.28"    | "0.21"    | "8651.23"     | "88922330.09" | 10000.0   | false
          "XRPBULL"     | "XRP"     | "USD"    | "TRADING" | "439.18"    | "4.32"             | "0.25"    | "0.28"    | "0.21"    | "8651.23"     | "88922330.09" | 10000.0   | false
          "ADADOWNUSDT" | "ADADOWN" | "USDT"   | "TRADING" | "23.18"     | "5.32"             | "22.23"   | "35.28"   | "17.21"   | "765.90"      | "789923.09"   | 10000.0   | false
          "ADAUPUSDT"   | "ADAUP"   | "USDT"   | "TRADING" | "33.19"     | "2.12"             | "35.29"   | "36.28"   | "18.21"   | "768.23"      | "789923.09"   | 10000.0   | false
          //verify that the service handles the case of no data being returned
          null          | null      | null     | null      | null        | null               | null      | null      | null      | null          | null          | null      | false
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
          "BTCUSD" | "24h"    | "1m"         | [expected: false]
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

    @Unroll
    def "test getExchangeInfoWithoutMarketCap"() {
        given:
          def exchangeInfo = new ExchangeInfo()
          def symbols = [new Symbol(symbol: symbol1, quoteAsset: quote1), new Symbol(symbol: symbol2, quoteAsset: quote2)]
          exchangeInfo.setSymbols(symbols)

        when:
          cacheUtil.retrieveFromCache(*_) >> exchangeInfo

        then:
          def info = service.getExchangeInfoWithoutMarketCap()

        expect:
          assert info
          assert info.getSymbols()
          if (inExchangeInfo1) {
              def coin = info.getSymbols().find { it.getSymbol() == symbol1 }
              assert coin
              assert coin.getQuoteAsset() == quote1
          } else {
              //test that the coin is not returned, since its currency "quote asset" is prohibited
              def coin = info.getSymbols().find { it.getSymbol() == symbol1 }
              assert !coin
          }
          if (inExchangeInfo2) {
              def coin = info.getSymbols().find { it.getSymbol() == symbol2 }
              assert coin
              assert coin.getQuoteAsset() == quote2
          } else {
              def coin = info.getSymbols().find { it.getSymbol() == symbol2 }
              assert !coin
          }

        where:
          symbol1 | quote1 | inExchangeInfo1 | symbol2 | quote2 | inExchangeInfo2
          "BTC"   | "USDT" | true            | "LTC"   | "USDC" | true
          //"EUR" is a non-supported quote (currency), so it won't get added to the exchange info
          "BTC"   | "EUR"  | false           | "LTC"   | "USDC" | true
    }

    def "test getExchangeInfoWithoutMarketCap() adds market cap and id"() {
        given:
          def symbolBtc = "BTC"
          def symbolLtc = "LTC"
          def marketCapBtc = 10000000
          def marketCapLtc = 300000
          def idBtc = 1
          def idLtc = 2

          def exchangeInfo = new ExchangeInfo()
          def symbols = [new Symbol(symbol: symbolBtc, id: idBtc, marketCap: marketCapBtc),
                         new Symbol(symbol: symbolLtc, id: idLtc, marketCap: marketCapLtc)]
          exchangeInfo.setSymbols(symbols)

          def coinMarketCapListing = new CoinMarketCapListing()
          def dataBtc = new CoinMarketCapData(symbol: symbolBtc)
          def dataLtc = new CoinMarketCapData(symbol: symbolLtc)
          def dataMap = [:] as HashMap<String, CoinMarketCapData>
          dataMap.put(symbolBtc, dataBtc)
          dataMap.put(symbolLtc, dataLtc)
          coinMarketCapListing.setData(dataMap)

        when:
          cacheUtil.retrieveFromCache(*_) >> exchangeInfo
          coinMarketCapService.getCoinMarketCapListing() >> coinMarketCapListing

        then:
          def info = service.getExchangeInfoWithoutMarketCap()

        expect:
          assert info
          assert info.getSymbols()

          def coinBtc = info.getSymbols().find { it.getSymbol() == symbolBtc }
          assert coinBtc
          assert coinBtc.getId() == idBtc
          assert coinBtc.getMarketCap() == marketCapBtc

          def coinLtc = info.getSymbols().find { it.getSymbol() == symbolLtc }
          assert coinLtc
          assert coinLtc.getId() == idLtc
          assert coinLtc.getMarketCap() == marketCapLtc
    }

    def "test getExchangeInfo() retrieves market cap data and adds market cap and id"() {
        given:
          def baseAssetBtc = "BTC"
          def baseAssetLtc = "LTC"
          def nameBtc = "Bitcoin"
          def nameLtc = "Litecoin"
          def symbolBtc = "BTCUSD"
          def symbolLtc = "LTCETH"
          def marketCapBtc = 10000000
          def marketCapLtc = 300000
          def idBtc = 1
          def idLtc = 2

          def exchangeInfo = new ExchangeInfo()
          def symbols = [new Symbol(symbol: symbolBtc, baseAsset: baseAssetBtc, id: idBtc, marketCap: marketCapBtc),
                         new Symbol(symbol: symbolLtc, baseAsset: baseAssetLtc, id: idLtc, marketCap: marketCapLtc)]
          exchangeInfo.setSymbols(symbols)

          def coinMarketCapListing = new CoinMarketCapListing()
          def dataBtc = new CoinMarketCapData(symbol: baseAssetBtc, name: nameBtc, id: idBtc, marketCap: marketCapBtc)
          def dataLtc = new CoinMarketCapData(symbol: baseAssetLtc, name: nameLtc, id: idLtc, marketCap: marketCapLtc)
          def dataMap = [:] as HashMap<String, CoinMarketCapData>
          dataMap.put(symbolBtc, dataBtc)
          dataMap.put(symbolLtc, dataLtc)
          coinMarketCapListing.setData(dataMap)

        when:
          cacheUtil.retrieveFromCache(*_) >> exchangeInfo
          coinMarketCapService.getCoinMarketCapListing() >> coinMarketCapListing

        then:
          def info = service.getExchangeInfo()

        expect:
          assert info
          assert info.getSymbols()

          def coinBtc = info.getSymbols().find { it.getSymbol() == symbolBtc }
          assert coinBtc
          assert coinBtc.getId() == idBtc
          assert coinBtc.getMarketCap() == marketCapBtc

          def coinLtc = info.getSymbols().find { it.getSymbol() == symbolLtc }
          assert coinLtc
          assert coinLtc.getId() == idLtc
          assert coinLtc.getMarketCap() == marketCapLtc
    }

    @Unroll("test that #symbol has #expectedCoin for coin")
    def "test getCoin"() {
        given:
          //the following represents exchange information - metadata about coins on an exchange
          def exchangeInfo = new ExchangeInfo()
          def exchangeSymbol1 = new Symbol(symbol: symbol, quoteAsset: currency)
          def exchangeSymbol2 = new Symbol(symbol: "XRPUSD", quoteAsset: "USD")
          def exchangeSymbols
          if (exchangeHasSymbol) {
              exchangeSymbols = [exchangeSymbol1, exchangeSymbol2]
          } else {
              //test the rare case when the exchange doesn't have the symbol
              // (if a coin is added just recently since the exchange information was called before being put in the cache)
              exchangeSymbols = [exchangeSymbol2]
          }
          exchangeInfo.setSymbols(exchangeSymbols)

        when:
          cacheUtil.retrieveFromCache(_, _, _) >> exchangeInfo

        then:
          def coin = service.getCoin(symbol)

        expect:
          coin == expectedCoin

        where:
          symbol    | currency | expectedCoin | exchangeHasSymbol
          "BTCUSD"  | "USD"    | "BTC"        | true
          "LTCUSDT" | "USDT"   | "LTC"        | true
          "ETHUSD"  | "USD"    | "ETH"        | false

    }

    @Unroll("test that #symbol has #expectedQuote for quote")
    def "test getQuote"() {
        given:
          //the following represents exchange information - metadata about coins on an exchange
          def exchangeInfo = new ExchangeInfo()
          def exchangeSymbol1 = new Symbol(symbol: symbol, quoteAsset: currency)
          def exchangeSymbol2 = new Symbol(symbol: "XRPUSD", quoteAsset: "USD")
          def exchangeSymbols
          if (exchangeHasSymbol) {
              exchangeSymbols = [exchangeSymbol1, exchangeSymbol2]
          } else {
              //test the rare case when the exchange doesn't have the symbol
              // (if a coin is added just recently since the exchange information was called before being put in the cache)
              exchangeSymbols = [exchangeSymbol2]
          }
          exchangeInfo.setSymbols(exchangeSymbols)

        when:
          cacheUtil.retrieveFromCache(_, _, _) >> exchangeInfo

        then:
          def quote = service.getQuote(symbol)

        expect:
          quote == expectedQuote

        where:
          symbol    | currency | expectedQuote | exchangeHasSymbol
          "BTCUSD"  | "USD"    | "USD"         | true
          "LTCUSDT" | "USDT"   | "USDT"        | true
          "ETHUSD"  | "USD"    | "USD"         | false

    }

    def "test setRsiForTickers() sets RSI for period length"() {
        given:
          def ticker1 = new CoinTicker(close: 100)
          def ticker2 = new CoinTicker(close: 200)
          def ticker3 = new CoinTicker(close: 100)
          def ticker4 = new CoinTicker(close: 200)
          def tickers = [ticker1, ticker2, ticker3, ticker4]

        when:
          service.setRsiForTickers(tickers, 2)

        then:
          assert ticker1.getRsi() == 0.0f
          assert ticker2.getRsi() == 0.0f
          assert ticker3.getRsi() != 0.0f
          assert ticker4.getRsi() != 0.0f
    }

    def "test getRsiTickerData"() {
        given:
          def coinTicker1 = new CoinTicker(symbol: "BTC", close: 10000.0)
          def coinTicker2 = new CoinTicker(symbol: "LTC", close: 175.0)
          def symbols = ["BTC"]
          def coinTickerList = [coinTicker1, coinTicker2]

        when:
          cacheUtil.retrieveFromCache(*_) >> coinTickerList

        then:
          def tickerData = service.getRsiTickerData(symbols)

        expect:
          //The service extracts data for "4-hour", "12-hour", and "24-hour" for each coin,
          //therefore, all we need to do is check that the service correctly made the calls.
          //3 times the ticker list size is what we are expecting
          assert tickerData.size() == 3 * coinTickerList.size()
    }

    @Unroll("test that the exchange visitor returns '#expectedResult' for #coin")
    def "test getExchangeVisitor"() {
        when:
          def visitor = service.getExchangeVisitor()

        then:
          def coinName = visitor.getName(coin)

        expect:
          coinName == expectedResult

        where:
          coin   | expectedResult
          "BTC"  | "BTC"
          "UNI"  | "Uniswap"
          "HNT"  | "Helium"
          "LINK" | "Chainlink"
          "CND"  | "Cindicator"
          "BQX"  | "VGX"
          "YOYO" | "YOYOW"
          "PHB"  | "PHX"
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