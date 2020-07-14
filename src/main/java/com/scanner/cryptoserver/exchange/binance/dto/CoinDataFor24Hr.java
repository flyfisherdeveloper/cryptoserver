package com.scanner.cryptoserver.exchange.binance.dto;

import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapMap;

import java.text.DecimalFormat;
import java.text.NumberFormat;

//This data class is used by Kotlin code - therefore Lombok annotations cannot be used.
public class CoinDataFor24Hr {
    private byte[] icon;
    //symbol of the coin, which includes the market (quote) such as LTCUSD, or BTCUSDT
    private String symbol;
    //the coin such as BTC, or ETH
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

    public byte[] getIcon() {
        return icon;
    }

    public void setIcon(byte[] icon) {
        this.icon = icon;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCoin() {
        return coin;
    }

    public void setCoin(String coin) {
        this.coin = coin;
    }

    public Double getMarketCap() {
        return marketCap;
    }

    public void setMarketCap(Double marketCap) {
        this.marketCap = marketCap;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Double getPriceChange() {
        return priceChange;
    }

    public void setPriceChange(Double priceChange) {
        this.priceChange = priceChange;
    }

    public Double getPriceChangePercent() {
        return priceChangePercent;
    }

    public void setPriceChangePercent(Double priceChangePercent) {
        this.priceChangePercent = priceChangePercent;
    }

    public Double getLastPrice() {
        return lastPrice;
    }

    public void setLastPrice(Double lastPrice) {
        this.lastPrice = lastPrice;
    }

    public Double getHighPrice() {
        return highPrice;
    }

    public void setHighPrice(Double highPrice) {
        this.highPrice = highPrice;
    }

    public Double getLowPrice() {
        return lowPrice;
    }

    public void setLowPrice(Double lowPrice) {
        this.lowPrice = lowPrice;
    }

    public Double getVolume() {
        return volume;
    }

    public void setVolume(Double volume) {
        this.volume = volume;
    }

    public Double getQuoteVolume() {
        return quoteVolume;
    }

    public void setQuoteVolume(Double quoteVolume) {
        this.quoteVolume = quoteVolume;
    }

    public Double getVolumeChangePercent() {
        return volumeChangePercent;
    }

    public void setVolumeChangePercent(Double volumeChangePercent) {
        this.volumeChangePercent = volumeChangePercent;
    }

    public Long getOpenTime() {
        return openTime;
    }

    public void setOpenTime(Long openTime) {
        this.openTime = openTime;
    }

    public Long getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(Long closeTime) {
        this.closeTime = closeTime;
    }

    public String getTradeLink() {
        return tradeLink;
    }

    public void setTradeLink(String tradeLink) {
        this.tradeLink = tradeLink;
    }

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
                .filter(c -> c.isCoin(coin))
                .findFirst()
                .ifPresent(cap -> setMarketCap(getMarketCapFormattedValue(cap.getMarketCap())));
    }
}
