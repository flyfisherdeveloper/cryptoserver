package com.scanner.myscanner.exchange.binance.us.dto;

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
    private String iconLink;

    public void setupLinks() {
        //get the url of the trading pair - this function only works for USD, USDT, BTC, ETH pairs
        //that is OK for now, but if there is ever a different pairing of 4 letters (such as LTCDOGE),
        //then that link won't work
        var newStr = coin + "_" + currency;
        var tradeUrl = "https://www.binance.us/en/trade/";
        tradeLink = tradeUrl + newStr;
        var iconUrl = "http://localhost:8080/api/v1/binance/icon/";
        iconLink = iconUrl + coin.toLowerCase();
    }
}
