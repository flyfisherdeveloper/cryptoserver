package com.scanner.cryptoserver;

import com.scanner.cryptoserver.exchange.binance.dto.ExchangeInfo;
import com.scanner.cryptoserver.exchange.binance.dto.Symbol;
import com.scanner.cryptoserver.exchange.binance.service.AbstractBinanceExchangeService;
import com.scanner.cryptoserver.exchange.coinmarketcap.CoinMarketCapService;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapMap;
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
    private final CoinMarketCapService coinMarketCapService;

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
                    binanceInfo.getSymbols().forEach(symbol -> addMarketCap(coinMarketCapInfo, symbol));
                    binanceUsaInfo.getSymbols().forEach(symbol -> addMarketCap(coinMarketCapInfo, symbol));
                    //return value is not needed, as the above future calls will save the data into the cache when called
                    return null;
                })
                .whenCompleteAsync((a, error) -> {
                    if (error == null) {
                        Log.info("Successfully completed retrieval of exchange info");
                    } else {
                        Log.error("Retrieval of exchange info error: {}", error.getMessage());
                    }
                });
    }

    //todo: this needs to be done for every exchange info call, not just on startup
    private void addMarketCap(CoinMarketCapMap coinMarketCapInfo, Symbol symbol) {
        //find the symbol (i.e. "BTC") in the coin market cap info, and get the market cap value from it and set it in the exchange symbol
        coinMarketCapInfo.getData()
                .stream()
                .filter(c -> c.getSymbol().equals(symbol.getSymbol()))
                .findFirst()
                .ifPresent(cap -> symbol.setMarketCap(cap.getMarketCap()));
    }
}
