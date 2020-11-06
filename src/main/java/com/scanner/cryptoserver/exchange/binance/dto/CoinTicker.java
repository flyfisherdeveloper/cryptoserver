package com.scanner.cryptoserver.exchange.binance.dto;

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
    private Double usdVolume;
    private Integer numberOfTrades;
    private double rsi;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Long getOpenTime() {
        return openTime;
    }

    public void setOpenTime(Long openTime) {
        this.openTime = openTime;
    }

    public String getOpenDate() {
        return openDate;
    }

    public void setOpenDate(String openDate) {
        this.openDate = openDate;
    }

    public Long getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(Long closeTime) {
        this.closeTime = closeTime;
    }

    public String getCloseDate() {
        return closeDate;
    }

    public void setCloseDate(String closeDate) {
        this.closeDate = closeDate;
    }

    public Double getOpen() {
        return open;
    }

    public void setOpen(Double open) {
        this.open = open;
    }

    public Double getHigh() {
        return high;
    }

    public void setHigh(Double high) {
        this.high = high;
    }

    public Double getLow() {
        return low;
    }

    public void setLow(Double low) {
        this.low = low;
    }

    public Double getClose() {
        return close;
    }

    public void setClose(Double close) {
        this.close = close;
    }

    public Double getVolume() {
        return volume;
    }

    public void setVolume(Double volume) {
        this.volume = volume;
    }

    public Double getQuoteAssetVolume() {
        return quoteAssetVolume;
    }

    public void setQuoteAssetVolume(Double quoteAssetVolume) {
        this.quoteAssetVolume = quoteAssetVolume;
    }

    public Integer getNumberOfTrades() {
        return numberOfTrades;
    }

    public void setNumberOfTrades(Integer numberOfTrades) {
        this.numberOfTrades = numberOfTrades;
    }

    public double getRsi() {
        return rsi;
    }

    public void setRsi(double rsi) {
        this.rsi = rsi;
    }

    public Double getUsdVolume() {
        return usdVolume;
    }

    public void setUsdVolume(Double usdVolume) {
        this.usdVolume = usdVolume;
    }
}
