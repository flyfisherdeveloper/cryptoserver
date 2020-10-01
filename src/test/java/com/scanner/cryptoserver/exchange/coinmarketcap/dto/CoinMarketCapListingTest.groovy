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

    def "test findData"() {
        given:
          def id1 = 1
          def symbol1 = "BTC"
          def name1 = "Bitcoin"
          def id2 = 2
          def symbol2 = "LTC"
          def name2 = "Litecoin"
          def data1 = new CoinMarketCapData(id: id1, symbol: symbol1, name: name1)
          def data2 = new CoinMarketCapData(id: id2, symbol: symbol2, name: name2)
          def data = [id1: data1, id2: data2]
          def list = new CoinMarketCapListing(data: data as Map)

        when:
          def foundBtc = list.findData("BTC")
          def foundLtc = list.findData("LTC")
          def foundEth = list.findData("ETH")
          def foundBitcoin = list.findData("Bitcoin")
          def foundEther = list.findData("Ether")

        then:
          assert foundBtc.isPresent()
          assert foundLtc.isPresent()
          assert !foundEth.isPresent()
          assert foundBitcoin.isPresent()
          assert !foundEther.isPresent()
    }
}
