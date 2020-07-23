package com.scanner.cryptoserver.exchange.coinmarketcap.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
public class CoinMarketCapListing {
    Map<Integer, CoinMarketCapData> data;

    /**
     * Convert a coin market cap data list to a coin market cap listing.
     * The purpose of this is to put the list of data in a Map for better lookup performance.
     *
     * @param data the list of coin market cap data - usually parsed from Json.
     * @return the listing with the map.
     */
    public CoinMarketCapListing convertToCoinMarketCapListing(List<CoinMarketCapData> data) {
        CoinMarketCapListing coinMarketCapListing = new CoinMarketCapListing();
        if (data == null || data.size() == 0) {
            return coinMarketCapListing;
        }
        Function<Integer, CoinMarketCapData> valueMapper = id -> data.stream()
                .filter(d -> d.getId() == id)
                .findFirst()
                //Here, we create an empty data object in case it can't be found.
                //This shouldn't happen, but do so just in case to prevent null pointer errors.
                .orElse(new CoinMarketCapData(0));
        //Create a map of coin market cap data keyed by coin market cap id
        Map<Integer, CoinMarketCapData> map = data.stream()
                .map(CoinMarketCapData::getId)
                .collect(Collectors.toMap(id -> id, valueMapper));
        coinMarketCapListing.setData(map);
        return coinMarketCapListing;
    }
}
