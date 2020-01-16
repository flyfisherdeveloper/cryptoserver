package com.scanner.cryptoserver.exchange;

import com.scanner.cryptoserver.exchange.binance.us.dto.CoinTicker;
import com.scanner.cryptoserver.util.IconExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.web.client.RestOperations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public abstract class AbstractExchangeService implements ExchangeService {
    private static final Logger Log = LoggerFactory.getLogger(AbstractExchangeService.class);

    private final CacheManager cacheManager;
    protected final RestOperations restTemplate;

    protected AbstractExchangeService(CacheManager cacheManager, RestOperations restTemplate) {
        this.cacheManager = cacheManager;
        this.restTemplate = restTemplate;
    }

    public byte[] getIconBytes(String coin) {
        //Attempt to get the icon out of the cache if it is in there.
        //If not in the cache, then call the icon extract service and add the icon bytes to the cache.
        //The data in the cache will expire according to the setup in the CachingConfig configuration.
        Cache iconCache = cacheManager.getCache("IconCache");
        byte[] coins = null;
        if (iconCache != null) {
            Cache.ValueWrapper value = iconCache.get(coin);
            if (value == null) {
                coins = IconExtractor.getIconBytes(coin);
                if (coins == null) {
                    //here, the coin icon wasn't in the zip file -
                    // add a non-null empty array to the cache so we don't keep trying to extract it from the zip file
                    coins = new byte[0];
                }
                iconCache.putIfAbsent(coin, coins);
            } else {
                coins = (byte[]) value.get();
            }
        }
        return coins;
    }

    protected List<CoinTicker> getTickerData(String coinCacheName, String symbol, String interval, String daysOrMonths) {
        //Attempt to get the data out of the cache if it is in there.
        //If not in the cache, then call the service and add the data to the cache.
        //The data in the cache will expire according to the setup in the CachingConfig configuration.
        Cache coinCache = cacheManager.getCache(coinCacheName);
        String name = symbol + interval + daysOrMonths;
        List<CoinTicker> coins;
        if (coinCache != null) {
            Cache.ValueWrapper value = coinCache.get(name);
            if (value == null) {
                coins = callCoinTicker(symbol, interval, daysOrMonths);
                coinCache.putIfAbsent(name, coins);
            } else {
                coins = (List<CoinTicker>) value.get();
            }
            return coins;
        }
        return new ArrayList<>();
    }

    protected List<CoinTicker> callCoinTicker(String symbol, String interval, String daysOrMonths) {
        Instant now = Instant.now();
        Instant from;
        long startTime1;
        long toTime1 = now.toEpochMilli();
        long startTime2 = 0;
        long toTime2 = 0;

        if (daysOrMonths.endsWith("d")) {
            int numDays = Integer.parseInt("" + daysOrMonths.charAt(0));
            from = now.minus(numDays, ChronoUnit.DAYS);
            startTime1 = from.toEpochMilli();
        } else {
            //If interval is 1-hour over 1-month, we need two calls.
            //This is because the Binance.us api only brings back 500 data points, but more than that are needed.
            //Therefore, two calls are needed - go back 15 days for one call, then 15 to 30 days for the other call.
            //Then, combine the results from the two calls, sorting on the close time.
            if (interval.equals("1h")) {
                Instant from15Days = now.minus(15, ChronoUnit.DAYS);
                startTime1 = from15Days.toEpochMilli();
                //todo: Instant does not support ChronoUnit.MONTHS - use 30 days as a workaround for now
                Instant from30Days = now.minus(30, ChronoUnit.DAYS);
                startTime2 = from30Days.toEpochMilli();
                toTime2 = startTime1;
            } else {
                //todo: Instant does not support ChronoUnit.MONTHS - use 30 days as a workaround for now
                from = now.minus(30, ChronoUnit.DAYS);
                startTime1 = from.toEpochMilli();
            }
        }
        List<CoinTicker> coinTickers = callCoinTicker(symbol, interval, startTime1, toTime1, startTime2, toTime2);
        return coinTickers;
    }

    private List<CoinTicker> callCoinTicker(String symbol, String interval, long startTime1, long toTime1, long startTime2, long toTime2) {
        List<CoinTicker> coinTickers = new ArrayList<>();

        if (startTime2 != 0 && toTime2 != 0) {
            CompletableFuture<List<CoinTicker>> call1 = CompletableFuture.supplyAsync(() -> callCoinTicker(symbol, interval, startTime1, toTime1));
            CompletableFuture<List<CoinTicker>> call2 = CompletableFuture.supplyAsync(() -> callCoinTicker(symbol, interval, startTime2, toTime2));
            CompletableFuture<Void> allCalls = CompletableFuture.allOf(call1, call2);
            allCalls.thenRun(() -> {
                try {
                    coinTickers.addAll(call1.get());
                    coinTickers.addAll(call2.get());
                    //sort the list - since they are run asynchronously, to ensure the final list is in order
                    coinTickers.sort(Comparator.comparingLong(CoinTicker::getCloseTime));
                } catch (InterruptedException | ExecutionException e) {
                    Log.error("Error: {} " + e.getMessage());
                }
            }).join();
        } else {
            coinTickers.addAll(callCoinTicker(symbol, interval, startTime1, toTime1));
        }
        return coinTickers;
    }

    /**
     * This is where the actual call to the exchange takes place. The Binance.us service will call its service,
     * the CoinbasePro service will call its service, etc.
     *
     * @param symbol    The symbol, such as "BTC-USD".
     * @param interval  The interval of time, such as "1d".
     * @param startTime The start time.
     * @param endTime    The end time.
     * @return A list of coin information for the time interval.
     */
    protected abstract List<CoinTicker> callCoinTicker(String symbol, String interval, Long startTime, Long endTime);
}
