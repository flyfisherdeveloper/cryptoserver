package com.scanner.myscanner.exchange.binance.us.dto;

import lombok.Data;

@Data
public class CoinDataFor24Hr {
    private String symbol;
    private Double priceChange;
    private Double priceChangePercent;
    private Double lastPrice;
    private Double highPrice;
    private Double lowPrice;
    private Double volume;
    private Double quoteVolume;
    private Long openTime;
    private Long closeTime;
}
