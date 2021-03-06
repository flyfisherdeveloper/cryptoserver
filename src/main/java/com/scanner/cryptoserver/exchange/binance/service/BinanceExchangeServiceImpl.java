package com.scanner.cryptoserver.exchange.binance.service;

import com.scanner.cryptoserver.exchange.coinmarketcap.CoinMarketCapService;
import com.scanner.cryptoserver.exchange.service.ExchangeVisitor;
import com.scanner.cryptoserver.util.CacheUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;

import java.util.concurrent.ScheduledExecutorService;

@Service(value = "binanceService")
public class BinanceExchangeServiceImpl extends AbstractBinanceExchangeService {
    private static final String EXCHANGE_NAME = "binance";

    private final BinanceUrlExtractor urlExtractor;
    private int tickerCounter = 0;
    private ScheduledExecutorService scheduledService;

    public BinanceExchangeServiceImpl(RestOperations restTemplate, BinanceUrlExtractor urlExtractor, CacheUtil cacheUtil, CoinMarketCapService coinMarketCapService, ExchangeVisitor exchangeVisitor) {
        super(restTemplate, coinMarketCapService, cacheUtil, exchangeVisitor);
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

    @Override
    protected String getUsdQuote() {
        return "USDT";
    }
}
