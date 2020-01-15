package com.scanner.cryptoserver;

import com.scanner.cryptoserver.exchange.binance.us.dto.ExchangeInfo;
import com.scanner.cryptoserver.exchange.coinbasepro.service.CoinbaseProExchangeServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CoinbaseProExchangeServiceImplIntegTest {
    @Autowired
    private CoinbaseProExchangeServiceImpl coinbaseProExchangeServiceImpl;

    @Test
    void testAllUsdSymbols() {
        ExchangeInfo exchangeInfo = coinbaseProExchangeServiceImpl.getExchangeInfo();
        System.out.println(exchangeInfo);
    }
}
