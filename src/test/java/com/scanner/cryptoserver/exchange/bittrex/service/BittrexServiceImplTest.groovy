package com.scanner.cryptoserver.exchange.bittrex.service

import com.scanner.cryptoserver.exchange.coinmarketcap.CoinMarketCapService
import com.scanner.cryptoserver.util.CacheUtil
import com.scanner.cryptoserver.util.UrlReader
import spock.lang.Specification


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

    def "test getMarkets"() {
        when:
          urlReader.readFromUrl() >> getJson()

        then:
          def markets = service.getMarkets()

        expect:
          assert markets
          def btc = markets.find { it.symbol == "BTC-USD" }
          assert btc
    }

    def getJson() {
        return "[{\"symbol\":\"BTC-USD\",\"high\":\"9543.23\",\"low\":\"9523.89\",\"volume\":\"1120.23\",\"quoteVolume\":\"193023.56\",\"updatedAt\":\"2020-07-02T21:05:17.837Z\"}]"
    }
}