package com.scanner.cryptoserver.exchange.coinmarketcap;

import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapData;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapListing;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertNotNull;

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
        CoinMarketCapData ltc = listing.getData().get("LTC");
        assertNotNull(ltc);
    }

    @Test
    void testGetMarketCapInfo() {
        Set<Integer> ids = new HashSet<>(Arrays.asList(1, 1027));
        CoinMarketCapListing info = service.getCoinMarketCapInfo(ids);
        assertNotNull(info);
        CoinMarketCapData btc = info.getData().get("BTC");
        assertNotNull(btc);
    }

    @Test
    void testGetCoinMarketCapListing() {
        Set<Integer> idSet = new HashSet<>();
        idSet.add(1);
        CoinMarketCapListing info = service.getCoinMarketCapListing(idSet);
        assertNotNull(info);
        CoinMarketCapData btc = info.getData().get("BTC");
        assertNotNull(btc);
    }
}
