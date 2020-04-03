package com.scanner.cryptoserver.exchange.coinmarketcap.dto;

import lombok.Data;

@Data
public class CoinMarketCapData {
    private int id;
    private String name;
    private String symbol;
    private String logo;
    private String description;
    private Double marketCap;
}
