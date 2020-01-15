package com.scanner.cryptoserver.exchange;

import com.scanner.cryptoserver.exchange.binance.us.dto.CoinTicker;
import com.scanner.cryptoserver.util.IconExtractor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.web.client.RestOperations;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractExchangeService implements ExchangeService {
    protected final CacheManager cacheManager;
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

    protected abstract List<CoinTicker> callCoinTicker(String symbol, String interval, String daysOrMonths);
}
