package com.scanner.cryptoserver;

import com.scanner.cryptoserver.exchange.binance.us.dto.CoinbaseProSymbol;
import com.scanner.cryptoserver.exchange.coinbasepro.CoinbaseProExchangeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

@SpringBootTest
class CoinbaseProExchangeServiceIntegTest {
    @Autowired
    private CoinbaseProExchangeService coinbaseProExchangeService;

    @Test
    void testAllUsdSymbols() {
        CoinbaseProSymbol[] exchangeInfo = coinbaseProExchangeService.getExchangeInfo();
        Arrays.stream(exchangeInfo).forEach(System.out::println);
    }
}
