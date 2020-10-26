package com.scanner.cryptoserver.util;

import com.scanner.cryptoserver.exchange.coinmarketcap.dto.ExchangeInfo;

import java.util.List;
import java.util.function.Supplier;

public interface CacheUtil {

    @FunctionalInterface
    interface CacheCommand {
        void run();
    }

    <T> T retrieveFromCache(String cacheName, String valueName, Supplier<T> supplier);

    ExchangeInfo retrieveExchangeInfoFromCache(String exchangeName, String cacheName, String valueName);

    void evictAndThen(String cacheName, String objectToEvict, CacheCommand command);

    void evict(String cacheName, String objectToEvict);

    byte[] getIconBytes(String coin, Integer id);

    void addExchangeInfoSupplier(String exchangeName, Supplier<ExchangeInfo> supplier);

    List<String> getExchangeNames();

    void putInCache(String cacheName, String valueName, Object cacheObject);
}
