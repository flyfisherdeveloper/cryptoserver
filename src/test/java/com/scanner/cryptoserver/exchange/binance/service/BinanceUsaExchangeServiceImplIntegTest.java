package com.scanner.cryptoserver.exchange.binance.service;

import com.scanner.cryptoserver.CachingConfig;
import com.scanner.cryptoserver.exchange.binance.controller.BinanceUsaExchangeController;
import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr;
import com.scanner.cryptoserver.exchange.binance.dto.CoinTicker;
import com.scanner.cryptoserver.exchange.bittrex.service.BittrexServiceImpl;
import com.scanner.cryptoserver.exchange.coinmarketcap.CoinMarketCapApiServiceImpl;
import com.scanner.cryptoserver.exchange.coinmarketcap.CoinMarketCapService;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.ExchangeInfo;
import com.scanner.cryptoserver.testutil.AbstractIntegTestSetup;
import com.scanner.cryptoserver.util.CacheUtilImpl;
import com.scanner.cryptoserver.util.IconExtractor;
import com.scanner.cryptoserver.util.UrlReader;
import com.scanner.cryptoserver.util.dto.Coin;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@WebMvcTest
class BinanceUsaExchangeServiceImplIntegTest extends AbstractIntegTestSetup {

    @Test
    void testAllUsdSymbols() {
        ExchangeInfo exchangeInfo = getBinanceUsaService().getExchangeInfo();
        List<Coin> usdCoins = exchangeInfo.getCoins().stream()
                .filter(s -> s.getQuoteAsset().equalsIgnoreCase("USD"))
                .collect(Collectors.toList());
        boolean allMatch = usdCoins.stream().allMatch(s -> s.getSymbol().endsWith("USD"));
        assertTrue(allMatch);
    }

    @Test
    void test24HrCoinTicker() {
        CoinDataFor24Hr data = getBinanceUsaService().get24HourCoinData("LTCUSD");
        assertEquals("LTC", data.getCoin());
        assertEquals("USD", data.getCurrency());
    }

    @Test
    void testCoinTicker() {
        List<CoinTicker> tickers = getBinanceUsaService().getCoinTicker("LTCBTC", "12h");
        for (CoinTicker ticker : tickers) {
            LocalDateTime openTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(ticker.getOpenTime()),
                    TimeZone.getDefault().toZoneId());
            LocalDateTime closeTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(ticker.getCloseTime()),
                    TimeZone.getDefault().toZoneId());
            System.out.println("open time: " + openTime);
            System.out.println("close time: " + closeTime);
            System.out.println(ticker);
            System.out.println();
        }
    }

    @Test
    void testExtractWithCoinName() {
        byte[] bytes = IconExtractor.getIconBytes("eth");
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void testExtractWithCoinId() {
        byte[] bytes = IconExtractor.getIconBytes("1958");
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }
}
