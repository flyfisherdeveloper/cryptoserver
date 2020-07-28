package com.scanner.cryptoserver.exchange.coinmarketcap.dto

import spock.lang.Specification
import spock.lang.Unroll

class CoinMarketCapListingTest extends Specification {

    def "test convertToCoinMarketCapListing"() {
        given:
          def data1 = new CoinMarketCapData(id: 1, symbol: "BTC", name: "Bitcoin")
          def data2 = new CoinMarketCapData(id: 2, symbol: "LTC", name: "Litecoin")
          def data3 = new CoinMarketCapData(id: 3, symbol: "ETH", name: "Ethereum")
          def data = [data3, data2, data1]

        when:
          def listing = new CoinMarketCapListing()

        then:
          def newListing = listing.convertToCoinMarketCapListing(data)

        expect:
          assert newListing
          assert newListing.getData()
          assert newListing.getData().size() == data.size()

          def btc = newListing.getData().get(1)
          assert btc

          def ltc = newListing.getData().get(2)
          assert ltc

          def eth = newListing.getData().get(3)
          assert eth
    }

    @Unroll
    def "test convertToCoinMarketCapListing() for empty list"() {
        when:
          def listing = new CoinMarketCapListing()

        then:
          def newListing = listing.convertToCoinMarketCapListing(data)

        expect:
          assert newListing
          assert newListing.getData() == null

        where:
          data                               | _
          new ArrayList<CoinMarketCapData>() | _
          null                               | _
    }
}
