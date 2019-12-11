package com.scanner.cryptoserver.exchange.binance.us.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ExchangeInfo {
    private List<Symbol> symbols;
}
