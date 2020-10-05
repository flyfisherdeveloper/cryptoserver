package com.scanner.cryptoserver.exchange.service;

import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr;
import com.scanner.cryptoserver.exchange.binance.dto.CoinTicker;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.ExchangeInfo;

import java.util.List;
import java.util.function.Supplier;

public interface ExchangeService {
    /**
     * Get the exchange info by calling the exchange API.
     *
     * @return the exchange info.
     */
    ExchangeInfo getExchangeInfo();

    ExchangeVisitor getExchangeVisitor();

    Supplier<ExchangeInfo> getExchangeInfoSupplier();

    /**
     * Retrieve the exchange info from the cache.
     * It is assumed that the exchange info is already in the cache.
     *
     * @return the exchange info.
     */
    ExchangeInfo retrieveExchangeInfoFromCache();

    CoinDataFor24Hr get24HourCoinData(String symbol);

    List<CoinDataFor24Hr> get24HrAllCoinTicker();

    List<CoinDataFor24Hr> get24HrAllCoinTicker(int page, int pageSize);

    List<CoinTicker> getTickerData(String symbol, String interval, String daysOrMonths);

    void setRsiForTickers(List<CoinTicker> tickers, int periodLength);

    List<CoinTicker> getRsiTickerData(List<String> symbols);
}

