package com.scanner.cryptoserver.coinbasepro.service

import com.scanner.cryptoserver.exchange.binance.us.dto.CoinbaseProSymbol
import com.scanner.cryptoserver.exchange.coinbasepro.service.CoinbaseProExchangeService
import com.scanner.cryptoserver.exchange.coinbasepro.service.CoinbaseProExchangeServiceAdapter
import spock.lang.Specification
import spock.lang.Unroll

class CoinbaseProExchangeServiceAdapterTest extends Specification {
    private CoinbaseProExchangeService coinbaseProExchangeService
    private CoinbaseProExchangeServiceAdapter adapter

    def setup() {
        coinbaseProExchangeService = Mock(CoinbaseProExchangeService)
        adapter = new CoinbaseProExchangeServiceAdapter(coinbaseProExchangeService)
    }

    @Unroll("Test adapting of #id to base currency of #baseCurrency and quote currency of #quoteCurrency")
    def "test getExchangeInfo"() {
        given:
        def info = new CoinbaseProSymbol()
        info.setBase_currency(baseCurrency)
        info.setId(id)
        info.setQuote_currency(quoteCurrency)
        def coinbaseProExchangeInfo = [info]

        when:
        coinbaseProExchangeService.getCoinbaseProExchangeInfo() >> coinbaseProExchangeInfo
        def exchangeInfo = adapter.getExchangeInfo()

        then:
        assert exchangeInfo
        assert exchangeInfo.getSymbols()
        assert exchangeInfo.getSymbols().size() == 1
        assert exchangeInfo.getSymbols().get(0).getSymbol() == id
        assert exchangeInfo.getSymbols().get(0).getBaseAsset() == baseCurrency
        assert exchangeInfo.getSymbols().get(0).getQuoteAsset() == quoteCurrency

        where:
        id    | baseCurrency | quoteCurrency
        "BTC" | "USD"        | "USD"
        "LTC" | "USD"        | "BTC"
    }
}