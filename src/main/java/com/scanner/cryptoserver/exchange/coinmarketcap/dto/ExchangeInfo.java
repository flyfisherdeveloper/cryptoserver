package com.scanner.cryptoserver.exchange.coinmarketcap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scanner.cryptoserver.util.dto.Symbol;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExchangeInfo {
    private List<Symbol> symbols;

    public List<Symbol> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<Symbol> symbols) {
        this.symbols = symbols;
    }
}
