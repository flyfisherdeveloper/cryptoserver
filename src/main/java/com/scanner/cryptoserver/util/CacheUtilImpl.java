package com.scanner.cryptoserver.util;

import com.scanner.cryptoserver.exchange.coinmarketcap.dto.ExchangeInfo;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service(value = "cacheUtil")
public class CacheUtilImpl implements CacheUtil {
    private final List<String> exchangeNames = new ArrayList<>();
    private final Map<String, Supplier<ExchangeInfo>> exchangeInfoSuppliersMap = new HashMap<>();
    private final CacheManager cacheManager;
    private static final String ICON_CACHE = "IconCache";

    public CacheUtilImpl(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Get something from a cache manager. If not already in the cache, call a supplier to supply the objects to be put in the cache.
     *
     * @param cacheName The name of the container in the cache manager.
     * @param valueName The name of the value wrapper in the container in the cache manager.
     * @param supplier  The supplier that is used to retrieve objects to be put in the cache.
     *                  This will be called if the object is not in the value wrapper.
     *                  If null, then it is assumed that the object is always in the cache;
     *                  If the supplier is null and the object is not in the cache, then the object returned will be null.
     *                  The client objects using this need to prevent this case.
     * @param <T>       The type of the element in the cache.
     * @return The element in the cache.
     */
    @Override
    public <T> T retrieveFromCache(String cacheName, String valueName, Supplier<T> supplier) {
        T cacheObj = null;
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            Cache.ValueWrapper value = cache.get(valueName);
            if (value == null && supplier != null) {
                cacheObj = supplier.get();
                cache.put(valueName, cacheObj);
            } else if (value != null) {
                cacheObj = (T) value.get();
            }
        }
        return cacheObj;
    }

    /**
     * Convenience method to get exchange info out of the cache manager.
     *
     * @param exchangeName The name of the exchange, such as "binanceusa".
     * @param cacheName    The name of the container in the cache manager.
     * @param valueName    The name of the value wrapper in the container in the cache manager.
     * @return The element in the cache.
     */
    @Override
    public ExchangeInfo retrieveExchangeInfoFromCache(String exchangeName, String cacheName, String valueName) {
        return retrieveFromCache(cacheName, valueName, exchangeInfoSuppliersMap.get(exchangeName));
    }

    /**
     * Evict all the objects in the cache if the cache exists. When done evicting, call a command,
     * which is usually to supply new objects for the cache, but can be anything.
     *
     * @param cacheName     The name of the cache to evict.
     * @param objectToEvict The name of the object to evict.
     * @param command       The command that will be called after eviction. This won't be called
     *                      if the cache does not exist, and eviction didn't happen.
     */
    @Override
    public void evictAndThen(String cacheName, String objectToEvict, CacheCommand command) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evictIfPresent(objectToEvict);
            command.run();
        }
    }

    /**
     * Evict from the cache if present.
     *
     * @param cacheName     The name of the cache.
     * @param objectToEvict The object to evict.
     */
    @Override
    public void evict(String cacheName, String objectToEvict) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evictIfPresent(objectToEvict);
        }
    }

    @Override
    public byte[] getIconBytes(String coin, Integer id) {
        //Attempt to get the icon out of the cache if it is in there.
        //If not in the cache, then call the icon extract service and add the icon bytes to the cache.
        //The data in the cache will expire according to the setup in the CachingConfig configuration.
        Supplier<byte[]> iconExtractor = () -> {
            byte[] coins = null;
            if (coin != null) {
                coins = IconExtractor.getIconBytes(coin);
            }
            if (coins == null && id != null) {
                coins = IconExtractor.getIconBytes(id);
            }
            if (coins == null) {
                //here, the coin icon wasn't in the images folder
                // add a non-null empty array to the cache so we don't keep trying to extract it
                coins = new byte[0];
            }
            return coins;
        };
        byte[] coins = retrieveFromCache(ICON_CACHE, coin == null ? id.toString() : coin, iconExtractor);
        return coins;
    }

    @Override
    public void addExchangeInfoSupplier(String exchangeName, Supplier<ExchangeInfo> supplier) {
        exchangeNames.add(exchangeName);
        exchangeInfoSuppliersMap.put(exchangeName, supplier);
    }

    @Override
    public List<String> getExchangeNames() {
        return exchangeNames;
    }

    @Override
    public void putInCache(String cacheName, String valueName, Object cacheObject) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(valueName, cacheObject);
        }
    }
}
