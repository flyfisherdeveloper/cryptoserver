package com.scanner.cryptoserver.exchange.binance.service;

import com.scanner.cryptoserver.exchange.coinmarketcap.CoinMarketCapService;
import com.scanner.cryptoserver.util.CacheUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;

import java.util.concurrent.ScheduledExecutorService;

@Service(value = "binanceUsaService")
public class BinanceUsaExchangeServiceImpl extends AbstractBinanceExchangeService {
    private static final String EXCHANGE_NAME = "binanceusa";

    private final BinanceUsaUrlExtractor urlExtractor;
    private int tickerCounter = 0;
    private ScheduledExecutorService scheduledService;

    public BinanceUsaExchangeServiceImpl(RestOperations restTemplate, BinanceUsaUrlExtractor urlExtractor, CacheUtil cacheUtil, CoinMarketCapService coinMarketCapService) {
        super(restTemplate, coinMarketCapService, cacheUtil);
        this.urlExtractor = urlExtractor;
        cacheUtil.addExchangeInfoSupplier(getExchangeName(), getExchangeInfoSupplier());
    }

    @Override
    protected UrlExtractor getUrlExtractor() {
        return urlExtractor;
    }

    @Override
    public String getExchangeName() {
        return EXCHANGE_NAME;
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
