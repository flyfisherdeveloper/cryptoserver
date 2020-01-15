package com.scanner.cryptoserver.exchange;

import com.scanner.cryptoserver.exchange.binance.us.dto.CoinDataFor24Hr;
import com.scanner.cryptoserver.exchange.binance.us.dto.CoinTicker;
import com.scanner.cryptoserver.exchange.binance.us.dto.ExchangeInfo;

import java.util.List;

public interface ExchangeService {
    ExchangeInfo getExchangeInfo();

    List<CoinTicker> getTickerData(String symbol, String interval, String daysOrMonths);

    CoinDataFor24Hr call24HrCoinTicker(String symbol);

    List<CoinDataFor24Hr> get24HrAllCoinTicker();

    byte[] getIconBytes(String coin);

    List<CoinTicker> getCoinTicker(String ltcbtc, String s);

    void add24HrVolumeChange(List<CoinDataFor24Hr> data);
}
