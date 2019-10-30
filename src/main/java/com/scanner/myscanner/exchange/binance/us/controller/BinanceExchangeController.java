package com.scanner.myscanner.exchange.binance.us.controller;

import com.scanner.myscanner.exchange.binance.us.dto.CoinDataFor24Hr;
import com.scanner.myscanner.exchange.binance.us.dto.CoinTicker;
import com.scanner.myscanner.exchange.binance.us.dto.ExchangeInfo;
import com.scanner.myscanner.exchange.binance.us.dto.Symbol;
import com.scanner.myscanner.exchange.binance.us.service.ExchangeService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("binance")
public class BinanceExchangeController {
    private final ExchangeService service;

    public BinanceExchangeController(ExchangeService service) {
        this.service = service;
    }

    @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin
    public List<Symbol> getExchangeInfo() {
        //return service.getExchangeInfo();
        ExchangeInfo info = service.getMockExchangeInfo();
        return info.getSymbols();
    }

    @GetMapping(value = "/24HourTicker/{symbol}", produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin
    public CoinDataFor24Hr get24HourTicker(@PathVariable String symbol) {
        //return service.get24HrCoinTicker(symbol);
        return service.getMock24HrCoinTicker(symbol);
    }

    @GetMapping(value = "/ticker/{symbol}/{interval}", produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin
    public List<CoinTicker> getCoinTicker(@PathVariable String symbol, @PathVariable String interval) {
        return service.getCoinTicker(symbol, interval);
    }
}
