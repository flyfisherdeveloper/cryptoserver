package com.scanner.cryptoserver;

import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr;
import com.scanner.cryptoserver.exchange.binance.dto.ExchangeInfo;
import com.scanner.cryptoserver.exchange.binance.dto.Symbol;
import com.scanner.cryptoserver.exchange.binance.service.AbstractBinanceExchangeService;
import com.scanner.cryptoserver.exchange.bittrex.service.BittrexServiceImpl;
import com.scanner.cryptoserver.exchange.coinmarketcap.CoinMarketCapService;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Component
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger Log = LoggerFactory.getLogger(ApplicationStartup.class);
    private final AbstractBinanceExchangeService binanceService;
    private final AbstractBinanceExchangeService binanceUsaService;
    private final BittrexServiceImpl bittrexService;
    private final CoinMarketCapService coinMarketCapService;

    public ApplicationStartup(AbstractBinanceExchangeService binanceService, AbstractBinanceExchangeService binanceUsaService, BittrexServiceImpl bittrexService,
                              CoinMarketCapService coinMarketCapService) {
        super();
        this.binanceService = binanceService;
        this.binanceUsaService = binanceUsaService;
        this.bittrexService = bittrexService;
        this.coinMarketCapService = coinMarketCapService;
    }

    /**
     * This event is executed as late as conceivably possible to indicate that
     * the application is ready to service requests.
     */
    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        String[] args = event.getArgs();
        if (args != null && args.length > 0 && args[0] != null && args[0].equals("sandbox")) {
            //run in sandbox mode - do not call exchange info here
            Log.info("Running Spring Boot application in Sandbox mode.");
            return;
        }
        //Asynchronously get the exchange information on startup.
        //Do not include the calls that fill the market cap, since that data hasn't been retrieved yet,
        //and will be retrieved when the threads finish retrieving the exchange info.
        CompletableFuture<ExchangeInfo> futureBinance = CompletableFuture.supplyAsync(binanceService::getExchangeInfoWithoutMarketCap);
        CompletableFuture<ExchangeInfo> futureBinanceUsa = CompletableFuture.supplyAsync(binanceUsaService::getExchangeInfoWithoutMarketCap);
        CompletableFuture<List<CoinDataFor24Hr>> futureBittrex = CompletableFuture.supplyAsync(bittrexService::getExchangeInfo);
        CompletableFuture.allOf(futureBinance, futureBinanceUsa, futureBittrex)
                .thenApplyAsync(dummy -> {
                    ExchangeInfo binanceInfo = futureBinance.join();
                    ExchangeInfo binanceUsaInfo = futureBinanceUsa.join();
                    List<CoinDataFor24Hr> bittrexInfo = futureBittrex.join();

                    Set<String> set = binanceInfo.getSymbols().stream().map(Symbol::getBaseAsset).collect(Collectors.toSet());
                    Set<String> usaSet = binanceUsaInfo.getSymbols().stream().map(Symbol::getBaseAsset).collect(Collectors.toSet());
                    Set<String> bittrexSet = bittrexInfo.stream().map(CoinDataFor24Hr::getCoin).collect(Collectors.toSet());
                    set.addAll(usaSet);
                    set.addAll(bittrexSet);
                    return set;
                })
                .whenComplete((coinSet, error) -> {
                    //now get the market cap value for each coin
                    CoinMarketCapMap coinMarketCapInfo = coinMarketCapService.getCoinMarketCapListingWithCoinSet(coinSet);

                    //Now fill the market cap for each coin on the exchanges.
                    //Here, we set the exchange info market cap for each coin, retrieving it from the coin market cap info.
                    try {
                        futureBinance.get().getSymbols().forEach(symbol -> symbol.addMarketCap(coinMarketCapInfo));
                        futureBinanceUsa.get().getSymbols().forEach(symbol -> symbol.addMarketCap(coinMarketCapInfo));
                        futureBittrex.get().forEach(coin -> coin.addMarketCap(coinMarketCapInfo));
                    } catch (InterruptedException | ExecutionException e) {
                        Log.error("Could not access exchange future for adding market cap: {}", e.getMessage());
                    }
                })
                .whenComplete((a, error) -> {
                    if (error == null) {
                        Log.info("Successfully completed retrieval of exchange info");
                    } else {
                        Log.error("Retrieval of exchange info error: {}", error.getMessage());
                    }
                });
    }
}
