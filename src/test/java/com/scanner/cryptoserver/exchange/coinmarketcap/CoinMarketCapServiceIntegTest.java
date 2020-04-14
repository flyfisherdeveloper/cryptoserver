package com.scanner.cryptoserver.exchange.coinmarketcap;

import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapData;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapMap;
import com.scanner.cryptoserver.util.CoinMarketCapApiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        CoinMarketCapMap map = apiService.getCoinMarketCapMap();
        assertNotNull(map);
        List<CoinMarketCapData> data = map.getData();
        data.forEach(System.out::println);
        boolean ltcFound = data.stream().anyMatch(d -> d.getSymbol().equals("LTC"));
        assertTrue(ltcFound);
    }

    @Test
    void testGetMarketCapInfo() {
        Set<Integer> ids = new HashSet<>(Arrays.asList(1, 1027));
        CoinMarketCapMap info = service.getCoinMarketCapInfo(ids);
        assertNotNull(info);
        info.getData().forEach(System.out::println);
        boolean btcFound = info.getData().stream().anyMatch(d -> d.getSymbol().equals("BTC"));
        assertTrue(btcFound);
    }

    @Test
    void testGetCoinMarketCapListing() {
        Set<Integer> idSet = new HashSet<>();
        idSet.add(1);
        CoinMarketCapMap info = service.getCoinMarketCapListing(idSet);
        info.getData().forEach(System.out::println);
        assertNotNull(info);
        boolean btcFound = info.getData().stream().anyMatch(d -> d.getSymbol().equals("BTC"));
        assertTrue(btcFound);
    }
}
