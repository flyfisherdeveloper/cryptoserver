package com.scanner.cryptoserver;

import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr;
import com.scanner.cryptoserver.exchange.binance.dto.CoinTicker;
import com.scanner.cryptoserver.exchange.binance.dto.ExchangeInfo;
import com.scanner.cryptoserver.exchange.binance.dto.Symbol;
import com.scanner.cryptoserver.exchange.binance.service.AbstractBinanceExchangeService;
import com.scanner.cryptoserver.exchange.binance.service.BinanceUsaExchangeService;
import com.scanner.cryptoserver.exchange.binance.service.BinanceUsaUrlExtractor;
import com.scanner.cryptoserver.util.IconExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BinanceUsaExchangeServiceIntegTest {
    @Autowired
    private BinanceUsaExchangeService exchangeService;
    @Autowired
    private AbstractBinanceExchangeService binanceExchangeService;
    @Autowired
    private BinanceUsaUrlExtractor urlExtractor;

    @Test
    void testAllUsdSymbols() {
        ExchangeInfo exchangeInfo = exchangeService.getExchangeInfo();
        List<Symbol> usdSymbols = exchangeInfo.getSymbols().stream()
                .filter(s -> s.getQuoteAsset().equalsIgnoreCase("USD"))
                .collect(Collectors.toList());
        boolean allMatch = usdSymbols.stream().allMatch(s -> s.getSymbol().endsWith("USD"));
        assertTrue(allMatch);
    }

    @Test
    void test24HrCoinTicker() {
        CoinDataFor24Hr data = exchangeService.call24HrCoinTicker("LTCUSD");
        assertEquals("LTC", data.getCoin());
        assertEquals("USD", data.getCurrency());
    }

    @Test
    void testCoinTicker() {
        List<CoinTicker> tickers = exchangeService.getCoinTicker("LTCBTC", "12h");
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
    void testExtract() {
        byte[] bytes = IconExtractor.getIconBytes("eth");
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void test24HrVolumeChange() {
        List<CoinDataFor24Hr> data = new ArrayList<>();
        CoinDataFor24Hr btc = new CoinDataFor24Hr();
        btc.setSymbol("BTCUSD");
        data.add(btc);

        binanceExchangeService.add24HrVolumeChange(data);
        assertNotNull(btc.getVolumeChangePercent());
    }
}
