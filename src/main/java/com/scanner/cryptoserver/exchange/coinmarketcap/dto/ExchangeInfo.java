package com.scanner.cryptoserver.exchange.coinmarketcap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scanner.cryptoserver.util.dto.Coin;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExchangeInfo {
    private List<Coin> coins;

    public List<Coin> getCoins() {
        return coins;
    }

    public void setCoins(List<Coin> coins) {
        this.coins = coins;
    }
}
