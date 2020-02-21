package com.scanner.cryptoserver.exchange.binance

import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr
import com.scanner.cryptoserver.exchange.binance.dto.ExchangeInfo
import com.scanner.cryptoserver.exchange.binance.dto.Symbol
import com.scanner.cryptoserver.exchange.binance.service.BinanceExchangeService
import com.scanner.cryptoserver.exchange.binance.service.BinanceUrlExtractor
import org.spockframework.lang.Wildcard
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
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
    private CacheManager cacheManager
    private Cache cache
    private BinanceUrlExtractor urlExtractor

    def setup() {
        restTemplate = Mock(RestTemplate)
        cacheManager = Mock(CacheManager)
        cache = Mock(Cache)
        urlExtractor = Mock(BinanceUrlExtractor)
        service = new BinanceExchangeService(restTemplate, cacheManager, urlExtractor)
    }

    def "test get24HrAllCoinTicker() when cache has data"() {
        given:
        def coinData = new CoinDataFor24Hr()
        def symbol = "BTCUSD"
        coinData.setSymbol(symbol)

        def valueWrapper = { -> return [coinData] }

        when:
        cacheManager.getCache(Wildcard.INSTANCE) >> cache
        cache.get(Wildcard.INSTANCE) >> valueWrapper
        def coins = service.get24HrAllCoinTicker()

        then:
        assert coins
        assert coins.size() == 1
        assert coins.get(0).getSymbol() == symbol
    }

    @Unroll
    def "test getMarkets"() {
        given:
        def exchangeInfo = new ExchangeInfo()
        def symbols = [new Symbol(baseAsset: coin1, quoteAsset: market1), new Symbol(baseAsset: coin2, quoteAsset: market2)]
        exchangeInfo.setSymbols(symbols)

        Cache.ValueWrapper valueMock = Mock(Cache.ValueWrapper)

        when: "mocks are called"
        cacheManager.getCache(Wildcard.INSTANCE) >> cache
        cache.get(Wildcard.INSTANCE) >> valueMock
        valueMock.get() >> exchangeInfo
        def markets = service.getMarkets()

        then:
        assert markets
        assert markets == expected.toSet()

        where:
        coin1 | market1 | coin2 | market2 | expected
        "BTC" | "USD"   | "LTC" | "USDC"  | ["USD", "USDC"]
        "BTC" | "USD"   | "LTC" | "USD"   | ["USD"]
    }

    //Here, we do a unit test of the 24Hour price ticker instead of an integration test
    // since an integration test would call the api and use up quota.
    @Unroll("Test that #symbol price info is correct for 24 Hour Coin Ticker service")
    def "test get24HrAllCoinTicker"() {
        given:
        def map = new LinkedHashMap()
        map["symbol"] = symbol
        map["priceChange"] = priceChange
        map["priceChangePercent"] = priceChangePercent
        map["lastPrice"] = lastPrice
        map["highPrice"] = highPrice
        map["lowPrice"] = lowPrice
        map["volume"] = volume
        map["quoteVolume"] = quoteVolume

        def coinList = serviceHasData ? [map] as LinkedHashMap<String, String>[] : [] as LinkedHashMap<String, String>[]
        def linkedHashMapResponse = ResponseEntity.of(Optional.of(coinList)) as ResponseEntity<LinkedHashMap[]>
        def mockCoinTickerResponse = getMockCoinTicker()

        def exchangeInfo = new ExchangeInfo()
        def symbols = [new Symbol(quoteAsset: "BTC"), new Symbol(quoteAsset: "USD"),
                       new Symbol(quoteAsset: "USDT"), new Symbol(quoteAsset: "BUSD")]
        exchangeInfo.setSymbols(symbols)
        def exchangeInfoResposne = ResponseEntity.of(Optional.of(exchangeInfo)) as ResponseEntity<ExchangeInfo>

        when: "mocks are called"
        cacheManager.getCache(Wildcard.INSTANCE) >> cache
        //cache returns null so that the call to the rest service can be made
        cache.get(Wildcard.INSTANCE) >> null
        //the following is mocking two calls to a rest service: the array returned contains the results of the two calls
        //the first item in the array is for the first call, the second item is for the second call
        restTemplate.getForEntity(Wildcard.INSTANCE, Wildcard.INSTANCE,) >>> [linkedHashMapResponse, exchangeInfoResposne]
        exchangeInfoResposne.getBody() >> coinList
        //note: the following are for making the test work - not testing here, but just needed for the test to run
        restTemplate.getForEntity(Wildcard.INSTANCE, Wildcard.INSTANCE, Wildcard.INSTANCE) >> mockCoinTickerResponse

        and: "the service is called"
        def allCoins = service.get24HrAllCoinTicker()

        then:
        assert allCoins != null
        assert allCoins.size() == coinList.size()
        if (serviceHasData) {
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
        } else {
            assert allCoins.isEmpty()
        }

        where:
        symbol    | coin  | currency | priceChange | priceChangePercent | lastPrice | highPrice | lowPrice  | volume        | quoteVolume | serviceHasData
        "LTCUSD"  | "LTC" | "USD"    | "13.2"      | "1.2"              | "14.3"    | "16.3"    | "11.17"   | "23987.23"    | "54.23"     | true
        "BTCBUSD" | "BTC" | "BUSD"   | "439.18"    | "4.32"             | "8734.56" | "8902.87" | "8651.23" | "88922330.09" | "10180.18"  | true
        "BTCUSDT" | "BTC" | "USDT"   | "439.18"    | "4.32"             | "8734.56" | "8902.87" | "8651.23" | "88922330.09" | "10180.18"  | true
        //verify that the service handles the case of no data being returned
        null      | null  | null     | null        | null               | null      | null      | null      | null          | null        | false
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
        restTemplate.getForEntity(Wildcard.INSTANCE, Wildcard.INSTANCE, Wildcard.INSTANCE) >> response
        response.getBody() >> coinData

        and: "the service is called"
        service.add24HrVolumeChange(coinList)

        then:
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
        restTemplate.getForEntity(Wildcard.INSTANCE, Wildcard.INSTANCE, Wildcard.INSTANCE) >>> [response1, response2]
        def tickers = null
        //the service call can throw an exception if too much data is requested: account for this
        try {
            tickers = service.callCoinTicker(symbol, interval, daysOrMonths)
        } catch (Exception e) {
            caughtException = e
        }

        then:
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
        def now = LocalDateTime.now()
        def closeTime1 = now.minusHours(2).toInstant(ZoneOffset.UTC).toEpochMilli()
        def closeTime2 = now.minusHours(1).toInstant(ZoneOffset.UTC).toEpochMilli()
        def response1 = getMockCoinTicker(closeTime1, closeTime2)

        def prevDayCloseTime1 = now.minusDays(1).minusHours(2).toInstant(ZoneOffset.UTC).toEpochMilli()
        def prevDayCloseTime2 = now.minusDays(1).minusHours(1).toInstant(ZoneOffset.UTC).toEpochMilli()
        def response2 = getMockCoinTicker(prevDayCloseTime1, prevDayCloseTime2)

        def coinTickerList = [response1, response2]
        def valueWrapper = { -> return coinTickerList }

        when:
        if (coinCacheExists) {
            cacheManager.getCache(Wildcard.INSTANCE) >> cache
        } else {
            cacheManager.getCache(Wildcard.INSTANCE) >> null
        }
        if (inCache) {
            cache.get(Wildcard.INSTANCE) >> valueWrapper
        } else {
            cache.get(Wildcard.INSTANCE) >> null
            //Here, response1 is the response for the first time the rest template is called; response2 is the second time the rest template is called.
            restTemplate.getForEntity(Wildcard.INSTANCE, Wildcard.INSTANCE, Wildcard.INSTANCE) >>> [response1, response2]
        }

        def coins = service.getTickerData(symbol, interval, daysOrMonths)

        then:
        assert coins != null
        if (coinCacheExists) {
            //if we pass this test, then we ensure that the coin was retrieved from the cache
            assert coins.size() == coinTickerList.size()
        } else {
            assert coins.size() == 0
        }

        where:
        symbol   | interval | daysOrMonths | coinCacheExists | inCache
        "BTCUSD" | "4h"     | "7d"         | true            | true
        "BTCUSD" | "4h"     | "7d"         | false           | false
        "BTCUSD" | "1h"     | "1d"         | true            | false
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