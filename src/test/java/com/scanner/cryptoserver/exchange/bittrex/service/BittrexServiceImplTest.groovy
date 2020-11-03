package com.scanner.cryptoserver.exchange.bittrex.service

import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr
import com.scanner.cryptoserver.exchange.bittrex.dto.Bittrex24HrData
import com.scanner.cryptoserver.exchange.coinmarketcap.CoinMarketCapService
import com.scanner.cryptoserver.util.CacheUtil
import com.scanner.cryptoserver.util.UrlReader
import spock.lang.Specification
import spock.lang.Unroll

import java.util.function.Supplier

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

    def "test getCoinDataFor24Hour using json supplier"() {
        when:
          urlReader.readFromUrl() >> getMarketsJson()
          //here, we mock the cache util, and use the supplier passed in as an argument
          cacheUtil.retrieveFromCache(*_) >> { args ->
              Supplier supplier = args.get(2)
              return supplier.get()
          }

        then:
          def markets = service.getCoinDataFor24Hour()

        expect:
          assert markets
          def btc = markets.find { it.symbol == "BTC-USD" }
          assert btc
          //getting the value from the json
          assert btc.getVolume() == 1120.23
    }

    def "test getCoinDataFor24Hour using mock removes 'EUR' market coins"() {
        given:
          def btcUsd = "BTC-USD"
          def btcUsdHigh = 8023.45

          def ethEur = "BTC-EUR"
          def ethEurHigh = 387.21

          def bittrexList = [new Bittrex24HrData(symbol: btcUsd, high: btcUsdHigh), new Bittrex24HrData(symbol: ethEur, high: ethEurHigh)]

        when:
          urlReader.readFromUrl() >> getMarketsJson()
          //mock the cache util, ignoring the json supplier in the service
          cacheUtil.retrieveFromCache(*_) >> bittrexList

        then:
          def markets = service.getCoinDataFor24Hour()

        expect:
          assert markets
          def btc = markets.find { it.getSymbol() == btcUsd }
          assert btc
          assert btc.getHighPrice() == btcUsdHigh

          //test that the EUR markets are eliminated
          def eth = markets.find { it.getSymbol() == ethEur }
          assert !eth
    }

    def "test get24HourCoinData"() {
        given:
          def symbol = "BTC-USD"

        when:
          urlReader.readFromUrl() >> getMarketsJson()
          //here, we mock the cache util, and use the supplier passed in as an argument
          cacheUtil.retrieveFromCache(*_) >> { args ->
              Supplier supplier = args.get(2)
              return supplier.get()
          }

        then:
          def coin = service.get24HourCoinData(symbol)

        expect:
          assert coin
          assert coin.getSymbol() == symbol
    }

    @Unroll("test that when the Bittrex exchange visitor is called for '#symbol' then the result is '#expectedResult'")
    def "test getExchangeVisitor"() {
        given:
          def coin = symbol

        when:
          def coinReference = service.getExchangeVisitor().getName(coin)

        then:
          assert coinReference
          assert coinReference == expectedResult

        where:
          symbol | expectedResult
          "BTC"  | "BTC"
          "UNI"  | "Uniswap"
    }

    def "test get24HrAllCoinTicker"() {
        when:
          urlReader.readFromUrl() >>> [getMarketsJson(), getTickersJson()]
          //here, we mock the cache util, and use the supplier passed in as an argument
          cacheUtil.retrieveFromCache(*_) >> { args ->
              Supplier supplier = args.get(2)
              return supplier.get()
          }
          //mock the call to set the market cap for each coin
          //sets the market cap to an arbitrary, non-zero value
          coinMarketCapService.setMarketCapDataFor24HrData(_, _) >> { args ->
              def list = args.get(1) as List<CoinDataFor24Hr>
              list.each { it.setMarketCap(10000.0) }
          }

        then:
          def coins = service.get24HrAllCoinTicker()

        expect:
          assert coins
          def btc = coins.find { it.symbol == "BTC-USD" }
          assert btc
          //getting the value from the json - "lastTradeRate" is converted to "lastPrice"
          assert btc.getLastPrice() == 9834.33

          //note: the full trade link is null in the service for a unit test, but the service adds the symbol
          //to the end of the trade link with the currency-coin pair
          //just test to ensure that the pair is added to the end of the trade link
          assert btc.tradeLink.endsWith("USD-BTC")
    }

    def "test getExchangeInfo"() {
        given:
          def symbolBtc = "BTC-USD"
          def symbolEth = "ETH-USD"
          def symbolEthEur = "ETH-EUR"
          def bittrex24HrData1 = new Bittrex24HrData(symbol: symbolBtc)
          def bittrex24HrData2 = new Bittrex24HrData(symbol: symbolEth)
          def bittrex24HrData3 = new Bittrex24HrData(symbol: symbolEthEur)
          def marketList = [bittrex24HrData1, bittrex24HrData2, bittrex24HrData3]

        when:
          cacheUtil.retrieveFromCache(*_) >> marketList

        then:
          def exchangeInfo = service.getExchangeInfo()

        expect:
          assert exchangeInfo
          assert exchangeInfo.getSymbols()
          assert exchangeInfo.getSymbols().size() == marketList.size() - 1

          def btc = exchangeInfo.getSymbols().find { it.getSymbol() == symbolBtc }
          assert btc
          assert btc.getSymbol() == symbolBtc
          assert btc.getQuoteAsset() == "USD"
          assert btc.getBaseAsset() == "BTC"

          def eth = exchangeInfo.getSymbols().find { it.getSymbol() == symbolEth }
          assert eth
          assert eth.getSymbol() == symbolEth
          assert eth.getQuoteAsset() == "USD"
          assert eth.getBaseAsset() == "ETH"

          //ensure that European markets don't get returned
          def ethEur = exchangeInfo.getSymbols().find { it.getSymbol() == symbolEthEur }
          assert !ethEur
    }

    def "test getMissingIcons"() {
        given:
          def coin1 = new CoinDataFor24Hr()
          def symbol1 = "BTCUSD"
          coin1.setSymbol(symbol1)

          def coin2 = new CoinDataFor24Hr()
          def symbol2 = "ETHUSD"
          coin2.setSymbol(symbol2)

          def coin3 = new CoinDataFor24Hr()
          def symbol3 = "XRPUSD"
          coin3.setSymbol(symbol3)

          def icon1 = new byte[3]
          icon1[0] = 'a'.getBytes()[0]
          icon1[1] = 'b'.getBytes()[0]
          icon1[2] = 'c'.getBytes()[0]
          def icon2 = new byte[0]
          def icon3 = null

        when:
          cacheUtil.retrieveFromCache(*_) >> [coin1, coin2, coin3]
          cacheUtil.getIconBytes(_, _) >>> [icon1, icon2, icon3]

        then:
          def coins = service.getMissingIcons()

        expect:
          assert coins
          assert coins.size() == 2
    }

    def getMarketsJson() {
        return "[{\"symbol\":\"BTC-USD\",\"high\":\"9543.23\",\"low\":\"9523.89\",\"volume\":\"1120.23\",\"quoteVolume\":\"193023.56\",\"updatedAt\":\"2020-07-02T21:05:17.837Z\"}]"
    }

    def getTickersJson() {
        return "[{\"symbol\":\"BTC-USD\",\"lastTradeRate\":\"9834.33\",\"bidRate\":\"9831.23\",\"askRate\":\"9833.89\"}]"
    }
}