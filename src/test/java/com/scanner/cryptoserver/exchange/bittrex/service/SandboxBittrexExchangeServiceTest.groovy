package com.scanner.cryptoserver.exchange.bittrex.service


import com.scanner.cryptoserver.util.SandboxUtil
import spock.lang.Specification

class SandboxBittrexExchangeServiceTest extends Specification {
    private SandboxBittrexExchangeService service
    private SandboxUtil sandboxUtil

    def setup() {
        sandboxUtil = new SandboxUtil()
        service = new SandboxBittrexExchangeService(sandboxUtil)
    }

    def "test get24HrAllCoinTicker"() {
        when:
          def tickers = service.get24HrAllCoinTicker()

        then:
          assert tickers
          assert tickers.size() > 0
          //make sure we find one that we know is in there
          def mtlBtc = tickers.find { it.symbol == "MTL-BTC" }
          assert mtlBtc
          assert mtlBtc.getHighPrice() > mtlBtc.getLowPrice()
    }
}