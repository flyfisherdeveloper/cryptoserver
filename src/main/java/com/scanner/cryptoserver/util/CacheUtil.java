package com.scanner.cryptoserver.util;

import com.scanner.cryptoserver.exchange.coinmarketcap.dto.ExchangeInfo;

import java.util.List;
import java.util.function.Supplier;

public interface CacheUtil {
    <T> T retrieveFromCache(String cacheName, String valueName, Supplier<T> supplier);

    ExchangeInfo retrieveExchangeInfoFromCache(String exchangeName, String cacheName, String valueName);

    void evictAndAdd(String cacheName, String objectToEvict, Supplier<?> supplier);

    void evict(String cacheName, String objectToEvict);

    byte[] getIconBytes(String coin);

    void addExchangeInfoSupplier(String exchangeName, Supplier<ExchangeInfo> supplier);

    List<String> getExchangeNames();

    void putInCache(String cacheName, String valueName, Object cacheObject);
}
