package com.scanner.cryptoserver.exchange.binance.service

import com.scanner.cryptoserver.exchange.service.ExchangeVisitor
import spock.lang.Specification
import spock.lang.Unroll

class BinanceExchangeVisitorTest extends Specification {
    private ExchangeVisitor visitor

    def setup() {
        visitor = new BinanceExchangeVisitor()
    }

    @Unroll("test that getName() returns '#expectedResult' for #coin")
    def "test getName"() {
        when:
          def coinName = visitor.getName(coin)

        then:
          assert coinName == expectedResult

        where:
          coin         | expectedResult
          "BTC"        | "BTC"
          "UNI"        | "Uniswap"
          "HNT"        | "Helium"
          "LINK"       | "Chainlink"
          "CND"        | "Cindicator"
          "BQX"        | "VGX"
          "YOYO"       | "YOYOW"
          "PHB"        | "PHX"
          "GXS"        | "GXC"
          "WNXM"       | "NXM"
          "HOT"        | "Holo"
          "COMP"       | "Compound"
          "Other Coin" | "Other Coin"
    }

    @Unroll("test that getSymbol() returns '#expectedResult' for #coin")
    def "test getSymbol"() {
        when:
          def coinSymbol = visitor.getSymbol(coin)

        then:
          assert coinSymbol == expectedResult

        where:
          coin         | expectedResult
          "BQX"        | "VGX"
          "YOYO"       | "YOYOW"
          "PHB"        | "PHX"
          "GXS"        | "GXC"
          "WNXM"       | "NXM"
          "GLM"        | "GNT"
          "Other Coin" | "Other Coin"
    }
}