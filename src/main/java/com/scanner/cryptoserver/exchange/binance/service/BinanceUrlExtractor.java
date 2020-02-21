package com.scanner.cryptoserver.exchange.binance.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BinanceUrlExtractor implements UrlExtractor {
    @Value("${exchanges.binance.info}")
    private String exchangeInfoUrl;
    @Value("${exchanges.binance.klines}")
    private String klinesUrl;
    @Value("${exchanges.binance.ticker}")
    private String tickerUrl;
    @Value("${exchanges.binance.trade}")
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
