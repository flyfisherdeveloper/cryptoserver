package com.scanner.cryptoserver.exchange.binance.service;

import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr;
import com.scanner.cryptoserver.exchange.binance.dto.CoinTicker;
import com.scanner.cryptoserver.exchange.binance.dto.ExchangeInfo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service(value = "sandboxBinanceUsaService")
public class SandboxBinanceUsaExchangeService implements BinanceExchangeService {

    @Override
    public ExchangeInfo getExchangeInfo() {
        return null;
    }

    @Override
    public CoinDataFor24Hr call24HrCoinTicker(String symbol) {
        return null;
    }

    @Override
    public List<CoinDataFor24Hr> get24HrAllCoinTicker() {
        return null;
    }

    @Override
    public List<CoinTicker> getTickerData(String symbol, String interval, String daysOrMonths) {
        return null;
    }
}
