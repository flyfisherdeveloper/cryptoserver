package com.scanner.cryptoserver.exchange.binance.service;

import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr;
import com.scanner.cryptoserver.exchange.binance.dto.CoinTicker;
import com.scanner.cryptoserver.exchange.binance.dto.ExchangeInfo;

import java.util.List;

public interface BinanceExchangeService {
    ExchangeInfo getExchangeInfo();

    CoinDataFor24Hr call24HrCoinTicker(String symbol);

    List<CoinDataFor24Hr> get24HrAllCoinTicker();

    List<CoinTicker> getTickerData(String symbol, String interval, String daysOrMonths);
}

