package com.scanner.myscanner.exchange.binance.us.dto;

import lombok.Data;

@Data
public class CoinTicker {
    private long openTime;
    private long closeTime;
    private String open;
    private String high;
    private String low;
    private String close;
    private String volume;
    private String quoteAssetVolume;
    private int numberOfTrades;
}
