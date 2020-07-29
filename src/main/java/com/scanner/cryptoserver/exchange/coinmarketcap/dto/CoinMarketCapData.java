package com.scanner.cryptoserver.exchange.coinmarketcap.dto;

import lombok.Data;

@Data
public class CoinMarketCapData {
    private int id;
    //name of the coin - such as "Bitcoin" or "Ethereum"
    private String name;
    //symbol of the coin, such as "BTC", or "ETH"
    private String symbol;
    //http path for a logo of the coin
    private String logo;
    //an English description of the coin
    private String description;
    //the market cap value in $USD for the coin
    private Double marketCap;

    public CoinMarketCapData() {

    }

    /**
     * Encapsulate a method to determine if a coin equals a coin symbol.
     * This is necessary since some coins have a symbol/currency pair different
     * from their symbol name. For example, for the coin IOTA, the symbol is MIOTA,
     * but the name is IOTA, while the pair is IOTA/BTC, or IOTA/USD, for example.
     * Usually, checking the coin symbol suffices. But if that fails, we need to check
     * for an exact match on the name.
     *
     * @param coinSymbol The coin symbol, such as "BTC" OR "IOTA".
     * @return true if this coin is equal to the coin symbol.
     */
    public boolean isCoin(String coinSymbol) {
        return getSymbol().equals(coinSymbol) || getName().equals(coinSymbol);
    }
}
