package com.scanner.cryptoserver.exchange.coinmarketcap.dto;

import lombok.Data;

import java.util.Map;

@Data
public class CoinMarketCapListing {
    Map<String, CoinMarketCapData> data;
}
