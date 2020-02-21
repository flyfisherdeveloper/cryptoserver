package com.scanner.cryptoserver;

import com.scanner.cryptoserver.exchange.binance.service.AbstractBinanceExchangeService;
import com.scanner.cryptoserver.exchange.binance.service.BinanceUsaExchangeService;
import com.scanner.cryptoserver.exchange.binance.service.UrlExtractor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {
    private final AbstractBinanceExchangeService binanceService;
    private final BinanceUsaExchangeService binanceUsaService;
    private final UrlExtractor binanceUrlExtractor;
    private final UrlExtractor binanceUsaUrlExtractor;

    public ApplicationStartup(AbstractBinanceExchangeService binanceService, BinanceUsaExchangeService binanceUsaService,
                              UrlExtractor binanceUrlExtractor, UrlExtractor binanceUsaUrlExtractor) {
        super();
        this.binanceService = binanceService;
        this.binanceUsaService = binanceUsaService;
        this.binanceUrlExtractor = binanceUrlExtractor;
        this.binanceUsaUrlExtractor = binanceUsaUrlExtractor;
    }

    /**
     * This event is executed as late as conceivably possible to indicate that
     * the application is ready to service requests.
     */
    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        //todo: add results to cache
        //binanceService.getExchangeInfo(binanceUrlExtractor);
        //binanceService.getExchangeInfo(binanceUsaUrlExtractor);
    }
}