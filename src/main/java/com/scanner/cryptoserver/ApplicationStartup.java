package com.scanner.cryptoserver;

import com.scanner.cryptoserver.exchange.binance.service.AbstractBinanceExchangeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger Log = LoggerFactory.getLogger(ApplicationStartup.class);
    private final AbstractBinanceExchangeService binanceService;
    private final AbstractBinanceExchangeService binanceUsaService;

    public ApplicationStartup(AbstractBinanceExchangeService binanceService, AbstractBinanceExchangeService binanceUsaService) {
        super();
        this.binanceService = binanceService;
        this.binanceUsaService = binanceUsaService;
    }

    /**
     * This event is executed as late as conceivably possible to indicate that
     * the application is ready to service requests.
     */
    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(binanceService::getExchangeInfo);
        CompletableFuture<Void> future2 = CompletableFuture.runAsync(binanceUsaService::getExchangeInfo);
        CompletableFuture<Void> allCalls = CompletableFuture.allOf(future1, future2);
        allCalls.thenRunAsync(() -> {
        }).whenCompleteAsync((a, error) -> {
            if (error == null) {
                Log.info("Successfully completed retrieval of exchange info");
            } else {
                Log.error("Retrieval of exchange info error: {}", error.getMessage());
            }
        });
    }
}