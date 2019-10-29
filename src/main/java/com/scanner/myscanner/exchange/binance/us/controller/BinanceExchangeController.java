package com.scanner.myscanner.exchange.binance.us.controller;

import com.scanner.myscanner.exchange.binance.us.dto.CoinDataFor24Hr;
import com.scanner.myscanner.exchange.binance.us.dto.CoinTicker;
import com.scanner.myscanner.exchange.binance.us.dto.ExchangeInfo;
import com.scanner.myscanner.exchange.binance.us.service.ExchangeService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("binance")
public class BinanceExchangeController {
    private final ExchangeService service;

    public BinanceExchangeController(ExchangeService service) {
        this.service = service;
    }

    @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ExchangeInfo getExchangeInfo() {
        return service.getExchangeInfo();
    }

    @GetMapping(value = "/24HourTicker/{symbol}", produces = MediaType.APPLICATION_JSON_VALUE)
    public CoinDataFor24Hr get24HourTicker(@PathVariable String symbol) {
        return service.get24HrCoinTicker(symbol);
    }

    @GetMapping(value = "/ticker/{symbol}/{interval}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CoinTicker> getCoinTicker(@PathVariable String symbol, @PathVariable String interval) {
        return service.getCoinTicker(symbol, interval);
    }
}
