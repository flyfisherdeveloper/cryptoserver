package com.scanner.cryptoserver.exchange.coinmarketcap.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
public class CoinMarketCapListing {
    Map<Integer, CoinMarketCapData> data;

    /**
     * Convert a coin market cap data list to a coin market cap listing.
     * The purpose of this is to put the list of data in a Map for better lookup performance.
     *
     * @param dataList the list of coin market cap data - usually parsed from Json.
     * @return the listing with the map.
     */
    public CoinMarketCapListing convertToCoinMarketCapListing(List<CoinMarketCapData> dataList) {
        CoinMarketCapListing coinMarketCapListing = new CoinMarketCapListing();
        if (dataList == null || dataList.size() == 0) {
            return coinMarketCapListing;
        }
        Map<Integer, CoinMarketCapData> map = dataList.stream().collect(Collectors.toMap(CoinMarketCapData::getId, d -> d));
        coinMarketCapListing.setData(map);
        return coinMarketCapListing;
    }

    /**
     * Find the coin market cap data using the symbol (i.e. "BTC").
     * The purpose of this method is to find the data when an id is not available.
     *
     * @param symbol the symbol - such as "ETH" or "LTC"
     * @return return the coin market cap data, if found.
     */
    public Optional<CoinMarketCapData> findData(String symbol) {
        return data.values().stream().filter(d -> d.isCoin(symbol)).findFirst();
    }
}
