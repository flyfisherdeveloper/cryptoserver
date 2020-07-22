package com.scanner.cryptoserver.util.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapData;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapListing;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Symbol {
    //the symbol of the coin, such as BTCUSDT, or LTCUSD
    private String symbol;
    //the base asset, such as BTC, or LTC
    private String baseAsset;
    //the market, or quote, of the asset such as USDT or USD
    private String quoteAsset;
    //status - whether the coin is trading, etc.
    private String status;
    //the market cap in $USD
    private Double marketCap = 0.0;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getBaseAsset() {
        return baseAsset;
    }

    public void setBaseAsset(String baseAsset) {
        this.baseAsset = baseAsset;
    }

    public String getQuoteAsset() {
        return quoteAsset;
    }

    public void setQuoteAsset(String quoteAsset) {
        this.quoteAsset = quoteAsset;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getMarketCap() {
        return marketCap;
    }

    public void setMarketCap(Double marketCap) {
        this.marketCap = marketCap;
    }

    /**
     * Add the market cap value from the coin market cap to the symbol.
     *
     * @param coinMarketCapInfo the coin market cap information which contains the symbol, which has the market cap value.
     */
    public void addMarketCap(CoinMarketCapListing coinMarketCapInfo) {
        //find the symbol (i.e. "BTC") in the coin market cap info, and get the market cap value from it and set it in the exchange symbol
        CoinMarketCapData data = coinMarketCapInfo.getData().get(getBaseAsset());
        if (data != null) {
            setMarketCap(data.getMarketCap());
        }
    }
}
