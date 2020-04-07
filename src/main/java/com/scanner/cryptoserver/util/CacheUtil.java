package com.scanner.cryptoserver.util;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.function.Supplier;

//todo: change this class to implement an interface so that unit testing is simpler (and better for SOLID principles)
public class CacheUtil {

    /**
     * Get something from a cache manager. If not already in the cache, call a supplier to supply the objects to be put in the cache.
     *
     * @param cacheManager The cache manager.
     * @param cacheName    The name of the container in the cache manager.
     * @param valueName    The name of the value wrapper in the container in the cache manager.
     * @param supplier     The supplier that is used to retrieve objects to be put in the cache.
     *                     This will be called if the object is not in the value wrapper.
     *                     If null, then it is assumed that the object is always in the cache;
     *                     If the supplier is null and the object is not in the cache, then the object returned will be null.
     *                     The client objects using this need to prevent this case.
     * @param <T>          The type of the element in the cache.
     * @return The element in the cache.
     */
    public static <T> T retrieveFromCache(CacheManager cacheManager, String cacheName, String valueName, Supplier<T> supplier) {
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
}
