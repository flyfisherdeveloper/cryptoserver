package com.scanner.myscanner.binance.us.service


import com.scanner.myscanner.exchange.binance.us.service.ExchangeService
import org.spockframework.lang.Wildcard
import org.springframework.cache.CacheManager
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant
import java.time.temporal.ChronoUnit

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
}