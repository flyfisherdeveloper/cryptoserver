package com.scanner.myscanner.exchange.binance.us.controller;

import com.scanner.myscanner.exchange.binance.us.dto.CoinDataFor24Hr;
import com.scanner.myscanner.exchange.binance.us.dto.CoinTicker;
import com.scanner.myscanner.exchange.binance.us.dto.ExchangeInfo;
import com.scanner.myscanner.exchange.binance.us.dto.Symbol;
import com.scanner.myscanner.exchange.binance.us.service.ExchangeService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import util.IconExtractor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("api/v1/binance")
public class BinanceExchangeController {
    private final ExchangeService service;

    public BinanceExchangeController(ExchangeService service) {
        this.service = service;
    }

    @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Symbol> getExchangeInfo() {
        //return service.getExchangeInfo();
        ExchangeInfo info = service.getMockExchangeInfo();
        return info.getSymbols();
    }

    @GetMapping(value = "/24HourTicker/{symbol}", produces = MediaType.APPLICATION_JSON_VALUE)
    public CoinDataFor24Hr get24HourTicker(@PathVariable String symbol) {
        return service.call24HrCoinTicker(symbol);
    }

    @GetMapping(value = "/24HourTicker", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CoinDataFor24Hr> getAll24HourTicker() {
        //return service.getMock24HrCoinTicker();
        return service.get24HrAllCoinTicker();
    }

    @GetMapping(value = "/Mock24HourTicker", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CoinDataFor24Hr> getMock24HourTicker() {
        return service.getMock24HrCoinTicker();
    }

    @GetMapping(value = "/ticker/{symbol}/{interval}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CoinTicker> getCoinTicker(@PathVariable String symbol, @PathVariable String interval) {
        return service.getCoinTicker(symbol, interval);
    }

    @GetMapping(value = "/7DayTicker/{symbol}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CoinTicker> get7DayTicker(@PathVariable String symbol) {
        //12-hour
        return service.get7DayTicker(symbol);
        //return service.getMock7DayTicker(symbol);
    }

    @GetMapping(value = "/7DayTicker/{symbol}/{interval}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CoinTicker> get7DayTicker(@PathVariable String symbol, @PathVariable String interval) {
        return service.get7DayTicker(symbol, interval);
    }

    @GetMapping(value = "/icon/{coin}", produces = MediaType.IMAGE_PNG_VALUE)
    public @ResponseBody byte[] getIcon(@PathVariable String coin) throws IOException {
        return service.getIconBytes(coin);
    }
}
