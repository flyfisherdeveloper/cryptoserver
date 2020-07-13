package com.scanner.cryptoserver.util;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Service(value = "cacheUtil")
public class CacheUtilImpl implements CacheUtil {
    private List<String> exchangeNames = new ArrayList<>();
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
     * Evict all the objects in the cache. Call a supplier to add objects to the cache.
     * The supplier has the responsibility to add objects to the cache.
     *
     * @param cacheName     The name of the cache to evict.
     * @param objectToEvict The name of the object to evict.
     * @param supplier      The supplier that will supply new objects to the cache after eviction.
     */
    @Override
    public void evictAndAdd(String cacheName, String objectToEvict, Supplier<?> supplier) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evictIfPresent(objectToEvict);
            supplier.get();
        }
    }

    /**
     * Evict from the cache if present.
     * @param cacheName The name of the cache.
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
    public byte[] getIconBytes(String coin) {
        //Attempt to get the icon out of the cache if it is in there.
        //If not in the cache, then call the icon extract service and add the icon bytes to the cache.
        //The data in the cache will expire according to the setup in the CachingConfig configuration.
        Supplier<byte[]> iconExtractor = () -> {
            byte[] coins = IconExtractor.getIconBytes(coin);
            if (coins == null) {
                //here, the coin icon wasn't in the images folder
                // add a non-null empty array to the cache so we don't keep trying to extract it
                coins = new byte[0];
            }
            return coins;
        };
        byte[] coins = retrieveFromCache(ICON_CACHE, coin, iconExtractor);
        return coins;
    }

    @Override
    public void addExchangeName(String exchangeName) {
        exchangeNames.add(exchangeName);
    }

    @Override
    public List<String> getExchangeNames() {
        return exchangeNames;
    }
}
