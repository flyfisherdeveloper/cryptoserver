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
}
