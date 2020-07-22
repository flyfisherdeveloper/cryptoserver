package com.scanner.cryptoserver.exchange.coinmarketcap.dto;

import lombok.Data;

import java.util.List;

@Data
public class CoinMarketCapListing {
    List<CoinMarketCapData> data;
}
