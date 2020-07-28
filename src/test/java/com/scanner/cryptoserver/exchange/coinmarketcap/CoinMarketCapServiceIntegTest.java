package com.scanner.cryptoserver.exchange.coinmarketcap;

import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapData;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapListing;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

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

    @Test
    void testGetMarketCapMap() {
        CoinMarketCapListing listing = apiService.getCoinMarketCapMap();
        assertNotNull(listing);
        Optional<CoinMarketCapData> ltc = findCoin(listing, "LTC");
        assertTrue(ltc.isPresent());
    }

    @Test
    void testGetMarketCapInfo() {
        Set<Integer> ids = new HashSet<>(Arrays.asList(1, 1027));
        CoinMarketCapListing listing = service.getCoinMarketCapInfo(ids);
        assertNotNull(listing);
        Optional<CoinMarketCapData> btc = findCoin(listing, "BTC");
        assertTrue(btc.isPresent());
    }

    @Test
    void testGetCoinMarketCapListing() {
        Set<Integer> idSet = new HashSet<>();
        idSet.add(1);
        CoinMarketCapListing listing = service.getCoinMarketCapListing(idSet);
        assertNotNull(listing);
        Optional<CoinMarketCapData> btc = findCoin(listing, "BTC");
        assertTrue(btc.isPresent());
    }

    private Optional<CoinMarketCapData> findCoin(CoinMarketCapListing listing, String coin) {
        return listing.getData().entrySet().stream()
                .filter(entry -> entry.getValue().isCoin(coin))
                .findFirst()
                .map(Map.Entry::getValue);
    }
}
