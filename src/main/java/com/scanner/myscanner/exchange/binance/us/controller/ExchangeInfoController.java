package com.scanner.myscanner.exchange.binance.us.controller;

import javafx.application.Application;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController(value = "/exchangeInfo")
public class ExchangeInfoController {

    @GetMapping
    public void getExchangeInfo() {

    }
}
