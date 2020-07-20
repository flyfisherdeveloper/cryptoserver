package com.scanner.cryptoserver.exchange.service;

import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr;
import com.scanner.cryptoserver.exchange.binance.dto.CoinTicker;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.ExchangeInfo;

import java.util.List;

public interface ExchangeService {
    ExchangeInfo getExchangeInfo();

    CoinDataFor24Hr get24HourCoinData(String symbol);

    List<CoinDataFor24Hr> get24HrAllCoinTicker();

    List<CoinDataFor24Hr> get24HrAllCoinTicker(int page, int pageSize);

    List<CoinTicker> getTickerData(String symbol, String interval, String daysOrMonths);

    void setRsiForTickers(List<CoinTicker> tickers, int periodLength);
}

