package com.scanner.cryptoserver.exchange.binance.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BinanceUsaUrlExtractor implements UrlExtractor {
    @Value("${exchanges.binanceusa.info}")
    private String exchangeInfoUrl;
    @Value("${exchanges.binanceusa.klines}")
    private String klinesUrl;
    @Value("${exchanges.binanceusa.ticker}")
    private String tickerUrl;
    @Value("${exchanges.binanceusa.trade}")
    private String tradeUrl;

    @Override
    public String getExchangeInfoUrl() {
        return exchangeInfoUrl;
    }

    @Override
    public String getKlinesUrl() {
        return klinesUrl;
    }

    @Override
    public String getTickerUrl() {
        return tickerUrl;
    }

    @Override
    public String getTradeUrl() {
        return tradeUrl;
    }
}
