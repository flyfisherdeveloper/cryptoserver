package com.scanner.myscanner.exchange.binance.us.controller;

import com.scanner.myscanner.exchange.binance.us.dto.ExchangeInfo;
import com.scanner.myscanner.exchange.binance.us.service.ExchangeService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BinanceExchangeController {
    private final ExchangeService service;

    public BinanceExchangeController(ExchangeService service) {
        this.service = service;
    }

    @GetMapping(value = "/binanceInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ExchangeInfo getExchangeInfo() {
        return service.getExchangeInfo();
    }
}
