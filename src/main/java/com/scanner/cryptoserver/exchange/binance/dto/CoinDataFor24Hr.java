package com.scanner.cryptoserver.exchange.binance.dto;

import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapMap;
import lombok.Data;

import java.text.DecimalFormat;
import java.text.NumberFormat;

@Data
public class CoinDataFor24Hr {
    private byte[] icon;
    private String symbol;
    private String coin;
    private Double marketCap = 0.0;
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

    private Double getMarketCapFormattedValue(Double marketCap) {
        NumberFormat nf = new DecimalFormat("##.##");
        return Double.parseDouble(nf.format(marketCap));
    }

    public void addMarketCap(CoinMarketCapMap coinMarketCapInfo) {
        //find the symbol (i.e. "BTC") in the coin market cap info, and get the market cap value from it and set it in the market cap field
        coinMarketCapInfo.getData()
                .stream()
                .filter(c -> c.getSymbol().equals(getCoin()))
                .findFirst()
                .ifPresent(cap -> setMarketCap(getMarketCapFormattedValue(cap.getMarketCap())));
    }
}
