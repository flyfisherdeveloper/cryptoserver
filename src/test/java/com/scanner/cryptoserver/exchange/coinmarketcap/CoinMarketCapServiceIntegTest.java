package com.scanner.cryptoserver.exchange.coinmarketcap;

import com.scanner.cryptoserver.exchange.binance.service.AbstractBinanceExchangeService;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapData;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapListing;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration test for calling Coin Market Cap api calls.
 * USE SPARINGLY!! This test uses up daily quota for api calls that are needed for production.
 * Only use this test when needed to avoid going over the api quota limits.
 */
@SpringBootTest
public class CoinMarketCapServiceIntegTest {
    @Autowired
    private CoinMarketCapService service;
    @Autowired
    private CoinMarketCapApiService apiService;
    @Autowired
    private AbstractBinanceExchangeService binanceService;

    @Test
    void testGetMarketCapMap() {
        CoinMarketCapListing listing = apiService.getCoinMarketCapMap();
        assertNotNull(listing);
        Optional<CoinMarketCapData> ltc = listing.findData("LTC", "Litecoin");
        assertTrue(ltc.isPresent());
    }

    @Test
    void testGetCoinMarketCapListing() {
        Set<Integer> idSet = new HashSet<>();
        idSet.add(1);
        CoinMarketCapListing listing = service.getCoinMarketCapListing(idSet);
        assertNotNull(listing);
        Optional<CoinMarketCapData> btc = listing.findData("BTC", "Bitcoin");
        assertTrue(btc.isPresent());
    }

    @Test
    void testGetIcons() {
        CoinMarketCapListing map = apiService.getCoinMarketCapMap();
        Set<String> set = map.getData().values().stream().map(CoinMarketCapData::getLogo).collect(Collectors.toSet());
        set.forEach(System.out::println);
    }
}
