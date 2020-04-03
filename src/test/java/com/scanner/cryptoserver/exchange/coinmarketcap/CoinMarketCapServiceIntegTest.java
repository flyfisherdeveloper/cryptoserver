package com.scanner.cryptoserver.exchange.coinmarketcap;

import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapData;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;

@SpringBootTest
public class CoinMarketCapServiceIntegTest {
    @Autowired
    private CoinMarketCapService service;

    @Test
    void testGetMarketCapMap() {
        CoinMarketCapMap map = service.getCoinMarketCapMap();
        assertNotNull(map);
        List<CoinMarketCapData> data = map.getData();
        data.forEach(System.out::println);
    }

    @Test
    void testGetMarketCapInfo() {
        List<Integer> ids = Arrays.asList(1, 1027);
        CoinMarketCapMap info = service.getCoinMarketCapInfo(ids);
        assertNotNull(info);
        info.getData().forEach(System.out::println);
    }

    @Test
    void testGetCoinMarketCapListing() {
        CoinMarketCapMap info = service.getCoinMarketCapListing();
        assertNotNull(info);
        info.getData().forEach(System.out::println);
    }
}
