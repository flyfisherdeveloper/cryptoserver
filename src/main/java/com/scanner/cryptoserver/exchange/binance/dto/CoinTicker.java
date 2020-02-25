package com.scanner.cryptoserver.exchange.binance.dto;

import lombok.Data;

@Data
public class CoinTicker {
    private String symbol;
    private Long openTime;
    private String openDate;
    private Long closeTime;
    private String closeDate;
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Double volume;
    private Double quoteAssetVolume;
    private Integer numberOfTrades;
}
