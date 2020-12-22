package com.scanner.cryptoserver.exchange.coinmarketcap;

import com.scanner.cryptoserver.exchange.binance.service.AbstractBinanceExchangeService;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapData;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapListing;
import com.scanner.cryptoserver.testutil.AbstractIntegTestSetup;
import com.scanner.cryptoserver.util.dto.Coin;
import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for calling Coin Market Cap api calls.
 * USE SPARINGLY!! This test uses up daily quota for api calls that are needed for production.
 * Only use this test when needed to avoid going over the api quota limits.
 */
@WebMvcTest
public class CoinMarketCapServiceIntegTest extends AbstractIntegTestSetup {
    @Autowired
    private CoinMarketCapService service;
    @Autowired
    private CoinMarketCapApiService apiService;
    @Autowired
    private AbstractBinanceExchangeService binanceService;

    @Test
    @Disabled
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

    //Use this test to retrieve coin logos.
    @Ignore
    void testGetIcons() {
        Set<Integer> set = binanceService.getExchangeInfo().getCoins().stream().map(Coin::getId).collect(Collectors.toSet());
        set.remove(null);
        CoinMarketCapListing listing = service.getCoinMarketCapInfoListing(set);
        listing.getData().values().forEach(d -> System.out.println(d.getLogo()));
    }
}
