package com.scanner.cryptoserver.exchange.binance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scanner.cryptoserver.CachingConfig;
import com.scanner.cryptoserver.exchange.binance.controller.BinanceUsaExchangeController;
import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr;
import com.scanner.cryptoserver.exchange.binance.dto.CoinTicker;
import com.scanner.cryptoserver.exchange.binance.dto.Symbol;
import com.scanner.cryptoserver.exchange.binance.service.BinanceUrlExtractor;
import com.scanner.cryptoserver.exchange.binance.service.BinanceUsaExchangeService;
import com.scanner.cryptoserver.exchange.binance.service.BinanceUsaUrlExtractor;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureMockMvc
@ContextConfiguration(classes = {BinanceUsaExchangeController.class, BinanceUsaExchangeService.class,
        BinanceUsaUrlExtractor.class, RestTemplate.class, CachingConfig.class, BinanceUrlExtractor.class})
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
}
