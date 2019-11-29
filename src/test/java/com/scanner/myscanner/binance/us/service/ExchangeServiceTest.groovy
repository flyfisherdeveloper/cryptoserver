package com.scanner.myscanner.binance.us.service

import com.scanner.myscanner.exchange.binance.us.dto.CoinDataFor24Hr
import com.scanner.myscanner.exchange.binance.us.dto.CoinTicker
import com.scanner.myscanner.exchange.binance.us.service.ExchangeService
import org.spockframework.lang.Wildcard
import org.springframework.cache.CacheManager
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate
import spock.lang.Specification
import spock.lang.Unroll

import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneOffset

class ExchangeServiceTest extends Specification {
    private ExchangeService service
    private RestOperations restTemplate
    private CacheManager cacheManager

    def setup() {
        restTemplate = Mock(RestTemplate)
        cacheManager = Mock(CacheManager)
        service = new ExchangeService(restTemplate, cacheManager)
    }

    //Here, we do a unit test of the 24Hour price ticker instead of an integration test
    // since an integration test would call the api and use up quota.
    @Unroll("Test that #symbol price info is correct for 24 Hour Coin Ticker service")
    def "test get24HrAllCoinTicker"() {
        given:
        def coin = new LinkedHashMap()
        coin["symbol"] = symbol
        coin["priceChange"] = priceChange
        coin["priceChangePercent"] = priceChangePercent
        coin["lastPrice"] = lastPrice
        coin["highPrice"] = highPrice
        coin["lowPrice"] = lowPrice
        coin["volume"] = volume
        coin["quoteVolume"] = quoteVolume

        def coinList = serviceHasData ? [coin] as LinkedHashMap<String, String>[] : [] as LinkedHashMap<String, String>[]
        def response = ResponseEntity.of(Optional.of(coinList)) as ResponseEntity<LinkedHashMap[]>

        when:
        restTemplate.getForEntity(Wildcard.INSTANCE, Wildcard.INSTANCE,) >> response
        response.getBody() >> coinList
        def allCoins = service.get24HrAllCoinTicker()

        then:
        assert allCoins != null
        assert allCoins.size() == coinList.size()
        if (serviceHasData) {
            assert allCoins[0].symbol == symbol
            assert allCoins[0].priceChange == priceChange as Double
            assert allCoins[0].priceChangePercent == priceChangePercent as Double
            assert allCoins[0].lastPrice == lastPrice as Double
            assert allCoins[0].highPrice == highPrice as Double
            assert allCoins[0].lowPrice == lowPrice as Double
            assert allCoins[0].volume == volume as Double
            assert allCoins[0].quoteVolume == quoteVolume as Double
        } else {
            assert allCoins.isEmpty()
        }

        where:
        symbol   | priceChange | priceChangePercent | lastPrice | highPrice | lowPrice  | volume        | quoteVolume | serviceHasData
        "LTCUSD" | "13.2"      | "1.2"              | "14.3"    | "16.3"    | "11.17"   | "23987.23"    | "54.23"     | true
        "BTCUSD" | "439.18"    | "4.32"             | "8734.56" | "8902.87" | "8651.23" | "88922330.09" | "10180.18"  | true
        //verify that the service handles the case of no data being returned
        null     | null        | null               | null      | null      | null      | null          | null        | false
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

    @Unroll("Test that when #symbol volume changes from #volumePrevious to #volumeNew that the expected volume percent change is #expectedVolumePercentChange%")
    def "test add24HrVolumeChange"() {
        given:
        def closeTime1 = LocalDateTime.of(2019, Month.DECEMBER, 15, 4, 17, 22)
        def closeTime2 = LocalDateTime.of(2019, Month.DECEMBER, 16, 4, 17, 22)

        def coin = new CoinDataFor24Hr()
        coin.setSymbol(symbol)
        def coinList = [coin]

        //The zeroes are just filler data - not needed for the test.
        def coinDataList1 = [0L, "0.0", "0.0", "0.0", "0.0", volumePrevious.toString(), closeTime1.toInstant(ZoneOffset.UTC).toEpochMilli(), "0.0", 0]
        def coinDataList2 = [0L, "0.0", "0.0", "0.0", "0.0", volumeNew.toString(), closeTime2.toInstant(ZoneOffset.UTC).toEpochMilli(), "0.0", 0]

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
        assert expectedVolumePercentChange == coinList[0].getVolumeChangePercent()

        where:
        symbol   | volumeNew | volumePrevious | expectedVolumePercentChange
        "BTCUSD" | 20.0      | 10.0           | 100.0
        "LTCUSD" | 10.0      | 20.0           | -50.0
    }
}