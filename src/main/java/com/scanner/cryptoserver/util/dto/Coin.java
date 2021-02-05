package com.scanner.cryptoserver.util.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapData;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapListing;
import com.scanner.cryptoserver.exchange.service.ExchangeVisitor;

import java.util.Arrays;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Coin {
    private static final String TRADING = "TRADING";
    private static final String SPOT = "SPOT";

    //the market cap id
    private Integer id;
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
    //Permissions include the type of trading for the coin.
    //Values include "SPOT", "MARGIN", "LEVERAGED", etc.
    private String[] permissions;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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

    public String[] getPermissions() {
        return permissions;
    }

    /**
     * Add the market cap and id value from the coin market cap to the symbol.
     *
     * @param exchangeVisitor used to determine which exact coin is wanted, given a list of coins with the same symbol.
     * @param listing         the coin market cap information which contains the symbol, which has the market cap and id value.
     */
    public void addMarketCapAndId(ExchangeVisitor exchangeVisitor, CoinMarketCapListing listing) {
        //find the symbol (i.e. "BTC") in the coin market cap info, and get the market cap value from it and set it in the exchange symbol
        Optional<CoinMarketCapData> data = listing.findData(exchangeVisitor.getSymbol(getBaseAsset()), exchangeVisitor.getName(getBaseAsset()));
        data.ifPresent(d -> {
            setMarketCap(d.getMarketCap());
            setId(d.getId());
        });
    }

    /**
     * Determine if a coin is a tradeable coin.
     *
     * @return true if a coin has "TRADING" as status and is a "SPOT" coin.
     */
    public boolean isTrading() {
        return getStatus().equals(TRADING) && Arrays.asList(getPermissions()).contains(SPOT);
    }
}
