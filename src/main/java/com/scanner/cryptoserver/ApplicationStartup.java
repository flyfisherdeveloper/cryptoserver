package com.scanner.cryptoserver;

import com.scanner.cryptoserver.exchange.binance.dto.ExchangeInfo;
import com.scanner.cryptoserver.exchange.binance.service.AbstractBinanceExchangeService;
import com.scanner.cryptoserver.exchange.coinmarketcap.CoinMarketCapService;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger Log = LoggerFactory.getLogger(ApplicationStartup.class);
    private final AbstractBinanceExchangeService binanceService;
    private final AbstractBinanceExchangeService binanceUsaService;
    private final CoinMarketCapService coinMarketCapService;
    private ScheduledExecutorService scheduledService;

    public ApplicationStartup(AbstractBinanceExchangeService binanceService, AbstractBinanceExchangeService binanceUsaService, CoinMarketCapService coinMarketCapService) {
        super();
        this.binanceService = binanceService;
        this.binanceUsaService = binanceUsaService;
        this.coinMarketCapService = coinMarketCapService;
    }

    /**
     * This event is executed as late as conceivably possible to indicate that
     * the application is ready to service requests.
     */
    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        //Asynchronously get the exchange information on startup.
        CompletableFuture<ExchangeInfo> futureBinance = CompletableFuture.supplyAsync(binanceService::getExchangeInfo);
        CompletableFuture<ExchangeInfo> futureBinanceUsa = CompletableFuture.supplyAsync(binanceUsaService::getExchangeInfo);
        CompletableFuture<CoinMarketCapMap> futureCoinMarketCap = CompletableFuture.supplyAsync(coinMarketCapService::getCoinMarketCapListing);
        CompletableFuture.allOf(futureBinance, futureBinanceUsa, futureCoinMarketCap)
                .thenApplyAsync(dummy -> {
                    ExchangeInfo binanceInfo = futureBinance.join();
                    ExchangeInfo binanceUsaInfo = futureBinanceUsa.join();
                    CoinMarketCapMap coinMarketCapInfo = futureCoinMarketCap.join();

                    //The binance api's do not return market cap info for each coin.
                    //Therefore, the call to the coin market cap exchange will get the market cap for the coins.
                    //Here, we set the binance exchange info market cap for each coin, retrieving it from the coin market cap info.
                    binanceInfo.getSymbols().forEach(symbol -> symbol.addMarketCap(coinMarketCapInfo));
                    binanceUsaInfo.getSymbols().forEach(symbol -> symbol.addMarketCap(coinMarketCapInfo));
                    //return value is not needed, as the above future calls will save the data into the cache when called
                    return null;
                })
                .whenCompleteAsync((a, error) -> {
                    if (error == null) {
                        Log.info("Successfully completed retrieval of exchange info");
                    } else {
                        Log.error("Retrieval of exchange info error: {}", error.getMessage());
                    }
                    startCoinMarketCapScheduler();
                });
    }

    //Run a scheduler to update the 24-hour coin market cap information.
    private void startCoinMarketCapScheduler() {
        Log.debug("Starting scheduler executor for Coin Market Cap exchange");
        scheduledService = Executors.newScheduledThreadPool(1);
        Runnable command = coinMarketCapService::getCoinMarketCapListing;
        //run every day - add a second to be sure we don't overuse quota on Coin Market Cap since that api keeps track of quota by the day
        scheduledService.scheduleAtFixedRate(command, 86401, 86401, TimeUnit.SECONDS);
    }
}
