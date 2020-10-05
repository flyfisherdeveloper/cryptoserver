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
          assert newListing.getData() != null
          assert newListing.getData().size() == 0

        where:
          data                               | _
          new ArrayList<CoinMarketCapData>() | _
          null                               | _
    }

    @Unroll
    def "test findData"() {
        given:
          def data1 = new CoinMarketCapData(id: id1, symbol: symbol1, name: name1)
          def data2 = new CoinMarketCapData(id: id2, symbol: symbol2, name: name2)
          def data = symbol1 == null ? null : [id1: data1, id2: data2]
          def list = new CoinMarketCapListing(data: data as Map)
          def emptyData = new CoinMarketCapData()

        when:
          def foundBySymbol1 = list.findData(symbol1, name1).orElse(emptyData)
          def foundBySymbol2 = list.findData(symbol2, name2).orElse(emptyData)
          def foundBySymbol1AndEmptyName = list.findData(symbol1, "").orElse(emptyData)
          def foundBySymbol2AndEmptyName = list.findData(symbol2, "").orElse(emptyData)
          def foundEth = list.findData("ETH", "Ether").orElse(emptyData)
          def foundEther = list.findData("Ether", "Ether").orElse(emptyData)

        then:
          assert foundBySymbol1.symbol == symbol1
          assert foundBySymbol1.name == name1

          assert foundBySymbol2.symbol == symbol2
          assert foundBySymbol2.name == name2

          assert foundEth.symbol == null
          assert foundEth.name == null

          assert foundEther.symbol == null
          assert foundEther.name == null

          assert foundBySymbol1AndEmptyName.symbol == symbol1
          assert foundBySymbol1AndEmptyName.name == name1

          assert foundBySymbol2AndEmptyName.symbol == symbol2
          if (symbol1 == symbol2) {
              //when there are duplicate symbols, the service returns the first one if the name is missing
              assert foundBySymbol2AndEmptyName.name == name1
          } else {
              assert foundBySymbol2AndEmptyName.name == name2
          }

        where:
          id1 | symbol1 | name1      | id2 | symbol2 | name2
          1   | "BTC"   | "Bitcoin"  | 2   | "LTC"   | "Litecoin"
          1   | "UNI"   | "Universe" | 2   | "UNI"   | "Uniswap"
          0   | null    | null       | 2   | null    | null
    }

    @Unroll
    def "test findData() that returns a list for multiple coins with identical symbols"() {
        given:
          def data1 = new CoinMarketCapData(symbol: symbol1, id: id1, name: name1)
          def data2 = new CoinMarketCapData(symbol: symbol2, id: id2, name: name2)
          def data3 = new CoinMarketCapData(symbol: symbol3, id: id3, name: name3)
          def data
          if (id1 == 0) {
              data = null
          } else {
              data = [id1: data1, id2: data2, id3: data3] as Map<Integer, CoinMarketCapData>
          }
          def listing = new CoinMarketCapListing(data: data)

        when:
          //find the data for "UNI" coin
          def list = listing.findData(symbol1)

        then:
          if (id1 == 0) {
              assert list != null
              assert list.size() == 0
          } else {
              //assert there are two distinct coins with symbol "UNI", but with different names
              assert list.size() == 2
              list.each { assert it.getSymbol() == symbol1 }
              def find = list.find { it.getId() == id1 }
              assert find.getName() == name1
              def find2 = list.find { it.getId() == id2 }
              assert find2.getName() == name2
          }

        where:
          id1 | symbol1 | name1      | id2 | symbol2 | name2     | id3 | symbol3 | name3
          1   | "UNI"   | "Universe" | 2   | "UNI"   | "Uniswap" | 3   | "BTC"   | "Bitcoin"
          //ids of 0 are used to create null data in the coin market cap data
          //we test this to ensure that there are no null pointer exceptions
          0   | null    | null       | 0   | null    | null      | 0   | null    | null
    }
}
