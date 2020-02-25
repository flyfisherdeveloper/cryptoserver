package com.scanner.cryptoserver.exchange.binance.service;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;

import java.util.concurrent.ScheduledExecutorService;

@Service(value = "binanceService")
public class BinanceExchangeService extends AbstractBinanceExchangeService {
    private static final String EXCHANGE_NAME = "binance";

    private final BinanceUrlExtractor urlExtractor;
    private int tickerCounter = 0;
    private ScheduledExecutorService scheduledService;

    public BinanceExchangeService(RestOperations restTemplate, CacheManager cacheManager, BinanceUrlExtractor urlExtractor) {
        super(restTemplate, cacheManager);
        this.urlExtractor = urlExtractor;
    }

    @Override
    protected UrlExtractor getUrlExtractor() {
        return urlExtractor;
    }

    @Override
    protected String getExchangeName() {
        return EXCHANGE_NAME;
    }

    @Override
    protected boolean getAdd24HrVolume() {
        return false;
    }

    @Override
    protected int getAll24HourTickerCount() {
        return tickerCounter;
    }

    @Override
    protected void incrementAll24HourTickerCounter() {
        tickerCounter++;
    }

    private void resetAll24HourTickerCounter() {
        tickerCounter = 0;
    }

    @Override
    protected void shutdownScheduledService() {
        scheduledService.shutdown();
        scheduledService = null;
        resetAll24HourTickerCounter();
    }

    @Override
    protected ScheduledExecutorService getScheduledService() {
        return scheduledService;
    }

    @Override
    protected void setScheduledService(ScheduledExecutorService scheduledService) {
        this.scheduledService = scheduledService;
    }
}
