package com.scanner.cryptoserver.util;

import java.util.List;
import java.util.function.Supplier;

public interface CacheUtil {
    <T> T retrieveFromCache(String cacheName, String valueName, Supplier<T> supplier);

    void evictAndAdd(String cacheName, String objectToEvict, Supplier<?> supplier);

    void evict(String cacheName, String objectToEvict);

    byte[] getIconBytes(String coin);

    void addExchangeName(String exchangeName);

    List<String> getExchangeNames();

    void putInCache(String cacheName, String valueName, Object cacheObject);
}
