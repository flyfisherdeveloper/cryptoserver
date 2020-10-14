package com.scanner.cryptoserver.util.dto

import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapData
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapListing
import com.scanner.cryptoserver.exchange.service.ExchangeVisitor
import org.jetbrains.annotations.NotNull
import spock.lang.Specification
import spock.lang.Unroll


class SymbolTest extends Specification {

    @Unroll
    def "test addMarketCapAndId"() {
        given:
          def visitor = new ExchangeVisitor() {
              @Override
              String getName(@NotNull String coin) {
                  if (coin == "UNI") {
                      return "Uniswap"
                  }
                  return coin
              }

              @Override
              String getSymbol(@NotNull String coin) {
                  if (coin.equals("BQX")) {
                      return "VGX"
                  }
                  if (coin.equals("YOYO")) {
                      return "YOYOW"
                  }
                  return coin
              }
          }

          def data1 = new CoinMarketCapData()
          data1.setId(id1)
          data1.setSymbol(baseAsset1)
          data1.setMarketCap(marketCap1)
          data1.setName(name1)

          def data2 = new CoinMarketCapData()
          data2.setId(id2)
          data2.setSymbol(baseAsset2)
          data2.setMarketCap(marketCap2)
          data2.setName(name2)

          def data = [id1: data1, id2: data2] as Map<Integer, CoinMarketCapData>
          def listing = new CoinMarketCapListing(data: data)

          def symbol1 = new Symbol(baseAsset: baseAsset1)
          def symbol2 = new Symbol(baseAsset: baseAsset2)

        when:
          symbol1.addMarketCapAndId(visitor, listing)
          symbol2.addMarketCapAndId(visitor, listing)

        then:
          assert symbol1.getId() == expectedId1
          assert symbol1.getMarketCap() == expectedMarketCap1
          assert symbol2.getId() == expectedId2
          assert symbol2.getMarketCap() == expectedMarketCap2

        where:
          //In the data here, for the second test, we mock the situation where the exchange has the coin "UNI", but
          //the market cap listing has numerous coins with symbol (base asset) of "UNI".
          //Therefore, the visitor will be used to determine which "UNI" coin the exchange wants, by supplying the
          // name of the coin, so that the symbol/name ("UNI"/"Uniswap") will be unique.
          //The symbol object will add the market cap and id based on the specific coin that is determined
          //through the visitor. This is reflected in the "expectedId" and "expectedMarketCap".
          id1 | baseAsset1 | name1      | visitorName1 | marketCap1 | expectedId1 | expectedMarketCap1 | id2 | baseAsset2 | name2      | visitorName2 | marketCap2 | expectedId2 | expectedMarketCap2
          1   | "BTC"      | "Bitcoin"  | "Bitcoin"    | 100000.00  | 1           | 100000.00          | 2   | "LTC"      | "Litecoin" | "Litecoin"   | 200000.0   | 2           | 200000.0
          1   | "UNI"      | "Universe" | "Universe"   | 50000.00   | 2           | 3000000.0          | 2   | "UNI"      | "Uniswap"  | "Uniswap"    | 3000000.0  | 2           | 3000000.0
    }
}