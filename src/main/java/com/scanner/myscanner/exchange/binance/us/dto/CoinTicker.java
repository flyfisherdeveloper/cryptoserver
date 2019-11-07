package com.scanner.myscanner.exchange.binance.us.dto;

import lombok.Data;

@Data
public class CoinTicker {
    private Long openTime;
    private String openDate;
    private Long closeTime;
    private String closeDate;
    private String open;
    private String high;
    private String low;
    private String close;
    private String volume;
    private String quoteAssetVolume;
    private Integer numberOfTrades;
}
