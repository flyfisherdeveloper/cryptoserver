package com.scanner.cryptoserver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scanner.cryptoserver.exchange.binance.controller.BinanceUsaExchangeController;
import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr;
import com.scanner.cryptoserver.exchange.binance.dto.CoinTicker;
import com.scanner.cryptoserver.exchange.binance.dto.Symbol;
import com.scanner.cryptoserver.exchange.binance.service.BinanceUrlExtractor;
import com.scanner.cryptoserver.exchange.binance.service.BinanceUsaExchangeService;
import com.scanner.cryptoserver.exchange.binance.service.BinanceUsaUrlExtractor;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureMockMvc
@ContextConfiguration(classes = {BinanceUsaExchangeController.class, BinanceUsaExchangeService.class,
        BinanceUsaUrlExtractor.class,
        RestTemplate.class, CachingConfig.class, BinanceUrlExtractor.class})
@WebMvcTest
class BinanceUsaExchangeControllerIntegTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private BinanceUsaExchangeService binanceUsaService;

    @Test
    void testExchangeInfo() throws Exception {
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                .get("/api/v1/binanceusa/info")
                .accept(MediaType.APPLICATION_JSON))
                .andReturn();

        String json = result.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();

        List<Symbol> symbols = mapper.readValue(json, new TypeReference<List<Symbol>>() {
        });
        assertTrue(symbols.size() > 0);

        //verify that bitcoin exists
        Optional<Symbol> btcUsd = symbols.stream().filter(s -> s.getSymbol().equals("BTCUSD")).findFirst();
        assertTrue(btcUsd.isPresent());
        //verify that the symbol is indeed bitcoin
        assertTrue(btcUsd.filter(b -> b.getBaseAsset().equals("BTC")).isPresent());
        //verify that the symbol is paired with USD
        assertTrue(btcUsd.filter(b -> b.getQuoteAsset().equals("USD")).isPresent());
    }

    @Test
    void testCoinFor24HourTicker() throws Exception {
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                .get("/api/v1/binanceusa/24HourTicker/LTCUSD")
                .accept(MediaType.APPLICATION_JSON))
                .andReturn();

        String json = result.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();

        CoinDataFor24Hr coin = mapper.readValue(json, CoinDataFor24Hr.class);
        assertNotNull(coin);
        assertEquals("LTC", coin.getCoin());
        assertEquals("USD", coin.getCurrency());
    }

    @Test
    void testDayTicker() throws Exception {
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                .get("/api/v1/binanceusa/DayTicker/DOGEUSDT/12h/1d")
                .accept(MediaType.APPLICATION_JSON))
                .andReturn();

        String json = result.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();

        List<CoinTicker> list = mapper.readValue(json, new TypeReference<List<CoinTicker>>() {
        });
        assertNotNull(list);
        assertFalse(list.isEmpty());
        CoinTicker coinTicker = list.get(0);
        assertNotNull(coinTicker.getVolume());
        assertNotNull(coinTicker.getQuoteAssetVolume());
    }

    @Test
    void testDayTickerFor3MonthsAnd4Hours() throws Exception {
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                .get("/api/v1/binanceusa/DayTicker/LTCUSD/4h/3m")
                .accept(MediaType.APPLICATION_JSON))
                .andReturn();

        String json = result.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();

        List<CoinTicker> list = mapper.readValue(json, new TypeReference<List<CoinTicker>>() {
        });
        assertNotNull(list);
        assertFalse(list.isEmpty());
        CoinTicker coinTicker = list.get(0);
        assertNotNull(coinTicker.getVolume());
        assertNotNull(coinTicker.getQuoteAssetVolume());
    }

    private LocalDateTime parseDate(String[] strs) {
        String dateStr = strs[0] + "T" + strs[1];
        return LocalDateTime.parse(dateStr);
    }

    private static final class DustInfo {
        long time;
        String timeStr;
        String coin;
        String amount;
        String fee;
        String convertedBnb;
        double openPrice;

        @Override
        public String toString() {
            return "DustInfo{" +
                    " timeStr=" + timeStr +
                    " time=" + time +
                    ", coin='" + coin + '\'' +
                    ", amount='" + amount + '\'' +
                    ", fee='" + fee + '\'' +
                    ", convertedBnb='" + convertedBnb + '\'' +
                    ", openPrice=" + openPrice +
                    '}';
        }
    }

    private List<DustInfo> getDust() throws URISyntaxException, IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource("files/dust.txt");
        Path path = null;
        if (url != null) {
            URI uri = url.toURI();
            path = Paths.get(uri);
        }
        //Date	Coin	Amount	Fee(BNB)	Converted BNB
        List<DustInfo> dust = new ArrayList<>();
        if (path != null) {
            Stream<String> lines = Files.lines(path);
            lines.forEach(line -> {
                String[] strs = line.split("\\s+");
                LocalDateTime localDateTime = parseDate(strs);
                Instant instant = localDateTime.toInstant(ZoneOffset.UTC);
                DustInfo d = new DustInfo();
                d.timeStr = strs[0] + " " + strs[1];
                d.time = instant.toEpochMilli();
                d.coin = strs[2];
                d.amount = strs[3];
                d.fee = strs[4];
                d.convertedBnb = strs[5];
                dust.add(d);
            });
        }
        return dust;
    }

    //https://api.binance.com/api/v3/klines?symbol=GVTBTC&interval=1m&startTime=1526337202000&endTime=1526337262000
    //def nextTime = LocalDateTime.of(2018, Month.MAY, 14, 22, 34, 22).toInstant(ZoneOffset.UTC).toEpochMilli()
    @Ignore
    void testDustConverter() throws URISyntaxException, IOException {
        List<DustInfo> dustList = getDust();
        Set<Long> times = new HashSet<>();
        dustList.forEach(dust -> {
            Long startTime = dust.time;
            Long endTime = dust.time + 60000;
            if (times.contains(startTime)) {
                Double openPrice = dustList.stream().filter(d -> d.time == startTime).findFirst().map(d -> d.openPrice).orElse(0.00);
                dust.openPrice = openPrice;
            } else {
                List<CoinTicker> coin = binanceUsaService.callCoinTicker("BNBBTC", "1m", startTime, endTime);
                times.add(startTime);
                dust.openPrice = coin.get(0).getOpen();
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(dust);
        });
    }
}
