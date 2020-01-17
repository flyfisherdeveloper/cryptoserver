package com.scanner.cryptoserver.exchange.coinbasepro.service

import com.scanner.cryptoserver.exchange.coinbasepro.Signature
import org.spockframework.lang.Wildcard
import org.springframework.cache.CacheManager
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestOperations
import spock.lang.Specification
import spock.lang.Unroll

class CoinbaseProExchangeServiceImplTest extends Specification {
    private RestOperations restTemplate
    private Signature signature
    private CacheManager cacheManager
    private CoinbaseProExchangeServiceImpl service

    def setup() {
        restTemplate = Mock(RestOperations)
        signature = Mock(Signature)
        cacheManager = Mock(CacheManager)
        service = new CoinbaseProExchangeServiceImpl(signature, cacheManager, restTemplate)
    }

    @Unroll("test 'callCoinTicker()' with #symbol and interval of #interval")
    def "test callCoinTicker"() {
        given:
        def coin = [(int) openTime, (double) low, (double) high, (double) open, (double) close, (double) volume]
        def coinList = new Object[1]
        coinList[0] = coin
        def response = ResponseEntity.of(Optional.of(coinList)) as ResponseEntity<Object[]>
        //startTime and endTime are arbitrary for this test
        def startTime = System.currentTimeMillis()
        def endTime = System.currentTimeMillis()

        when:
        restTemplate.getForEntity(Wildcard.INSTANCE, Wildcard.INSTANCE) >> response
        def coins = service.callCoinTicker(symbol, interval, startTime, endTime)

        then:
        assert coins != null
        if (interval.endsWith("min")) {
            assert coins.size() == 0
        } else {
            assert coins.size() == 1
            assert coins.get(0).getLow() == low
            assert coins.get(0).getHigh() == high
            assert coins.get(0).getOpen() == open
            assert coins.get(0).getClose() == close
            assert coins.get(0).getVolume() == volume
            int index = interval.indexOf("h")
            def hours = Integer.valueOf(interval.substring(0, index))
            assert coins.get(0).getCloseTime() == openTime + hours * 3600000
        }
        println coins

        where:
        symbol   | interval | openTime   | low    | high   | open   | close  | volume
        "BTCUSD" | "24h"    | 1579211327 | 123.33 | 130.40 | 127.33 | 128.18 | 6000
        "LTCUSD" | "6h"     | 1579211327 | 50.33  | 63.40  | 51     | 63.18  | 1344
        "BCHUSD" | "1h"     | 1579211327 | 324.33 | 335    | 328.19 | 330.18 | 950.27
        "BTCUSD" | "15min"  | 1579211327 | 123.33 | 130.40 | 127.33 | 128.18 | 6000
    }
}
