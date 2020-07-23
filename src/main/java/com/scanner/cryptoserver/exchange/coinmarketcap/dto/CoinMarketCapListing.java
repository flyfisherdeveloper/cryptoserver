package com.scanner.cryptoserver.exchange.coinmarketcap.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;
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
}
