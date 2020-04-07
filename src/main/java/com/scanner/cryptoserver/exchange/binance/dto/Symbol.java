package com.scanner.cryptoserver.exchange.binance.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapMap;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Symbol {
    private String symbol;
    private String baseAsset;
    private String quoteAsset;
    private String status;
    private Double marketCap = 0.0;

    /**
     * Add the market cap value from the coin market cap to the symbol.
     *
     * @param coinMarketCapInfo the coin market cap information which contains the symbol, which has the market cap value.
     */
    public void addMarketCap(CoinMarketCapMap coinMarketCapInfo) {
        //find the symbol (i.e. "BTC") in the coin market cap info, and get the market cap value from it and set it in the exchange symbol
        coinMarketCapInfo.getData()
                .stream()
                .filter(c -> c.getSymbol().equals(getBaseAsset()))
                .findFirst()
                .ifPresent(cap -> setMarketCap(cap.getMarketCap()));
    }
}
