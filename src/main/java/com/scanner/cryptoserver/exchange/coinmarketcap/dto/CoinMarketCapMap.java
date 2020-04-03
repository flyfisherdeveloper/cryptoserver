package com.scanner.cryptoserver.exchange.coinmarketcap.dto;

import lombok.Data;

import java.util.List;

@Data
public class CoinMarketCapMap {
    List<CoinMarketCapData> data;
}
