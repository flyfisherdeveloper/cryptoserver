package com.scanner.cryptoserver.exchange.binance.service;

public interface UrlExtractor {
    String getExchangeInfoUrl();

    String getKlinesUrl();

    String getTickerUrl();

    String getTradeUrl();
}
