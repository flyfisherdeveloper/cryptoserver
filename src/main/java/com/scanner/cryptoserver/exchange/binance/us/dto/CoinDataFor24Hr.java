package com.scanner.cryptoserver.exchange.binance.us.dto;

import lombok.Data;

@Data
public class CoinDataFor24Hr {
    private byte[] icon;
    private String symbol;
    private String coin;
    private String currency;
    private Double priceChange;
    private Double priceChangePercent;
    private Double lastPrice;
    private Double highPrice;
    private Double lowPrice;
    private Double volume;
    private Double quoteVolume;
    private Double volumeChangePercent;
    private Long openTime;
    private Long closeTime;
    private String tradeLink;

    public void setupLinks(String tradeUrl) {
        //get the url of the trading pair - this function only works for USD, USDT, BTC, ETH pairs
        //that is OK for now, but if there is ever a different pairing of 4 letters (such as LTCDOGE),
        //then that link won't work
        String newStr = coin + "_" + currency;
        tradeLink = tradeUrl + newStr;
    }
}
