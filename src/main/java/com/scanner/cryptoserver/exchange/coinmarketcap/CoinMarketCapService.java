package com.scanner.cryptoserver.exchange.coinmarketcap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapData;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapListing;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.ExchangeInfo;
import com.scanner.cryptoserver.exchange.service.ExchangeVisitor;
import com.scanner.cryptoserver.util.CacheUtil;
import com.scanner.cryptoserver.util.dto.Coin;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Encapsulates services that use the Coin Market Cap api.
 * Used mainly to get the total market cap (not exclusive to a specific exchange) for each coin.
 */
@Service
public class CoinMarketCapService {
    private static final Logger Log = LoggerFactory.getLogger(CoinMarketCapData.class);
    private static final String MARKET_CAP_MAP = "MarketCap";
    private static final String COIN_MARKET_CAP = "CoinMarketCap";
    private static final String LISTING = "Listing";
    private static final String INFO = "Info";
    private static final String EXCHANGE_INFO = "ExchangeInfo";

    private final CoinMarketCapApiService coinMarketCapApiService;
    private final CacheUtil cacheUtil;

    public CoinMarketCapService(CoinMarketCapApiService coinMarketCapApiService, CacheUtil cacheUtil) {
        this.coinMarketCapApiService = coinMarketCapApiService;
        this.cacheUtil = cacheUtil;
    }

    /**
     * Get a set of IDs that represent coins. Use the exchange info from the exchange services to get the coin names.
     *
     * @param visitor the exchange visitor used to use an alternate symbol to
     *                match what the coin market cap is expecting.
     * @return a Set of IDs.
     */
    public Set<Integer> getIdSet(ExchangeVisitor visitor) {
        Set<String> coinSet = new HashSet<>();
        //get the exchange info for all exchanges - this is to get a list of coins to retrieve the market cap for each coin
        cacheUtil.getExchangeNames().forEach(exchangeName -> {
            String name = exchangeName + "-" + EXCHANGE_INFO;
            ExchangeInfo exchangeInfo = cacheUtil.retrieveExchangeInfoFromCache(exchangeName, EXCHANGE_INFO, name);
            if (exchangeInfo != null) {
                coinSet.addAll(exchangeInfo.getCoins().stream().map(Coin::getBaseAsset).collect(Collectors.toSet()));
            }
        });
        return getIdSet(coinSet, visitor);
    }

    /**
     * Get a set of Coin Market Cap Ids for a set of coins.
     *
     * @param coinSet the coin set that will be used to look up the Ids.
     * @param visitor the exchange visitor used to use an alternate symbol to
     *                match what the coin market cap is expecting.
     * @return the set of Ids, based on the Coin Market Cap service.
     */
    public Set<Integer> getIdSet(Set<String> coinSet, ExchangeVisitor visitor) {
        Supplier<CoinMarketCapListing> supplier = coinMarketCapApiService::getCoinMarketCapMap;
        CoinMarketCapListing coinMarketCap = cacheUtil.retrieveFromCache(COIN_MARKET_CAP, MARKET_CAP_MAP, supplier);

        //now get a set of ids for the coins in the exchanges
        //note: there can be duplicate symbols in there, such as "UNI" (Universe) and "UNI" (Uniswap)
        Set<Integer> idSet = new HashSet<>();
        coinSet.forEach(coin -> {
            Set<Integer> ids = coinMarketCap.findData(coin, visitor).stream().map(CoinMarketCapData::getId).collect(Collectors.toSet());
            idSet.addAll(ids);
        });
        //Note. Id of 6999 is causing the coin market cap API to return a 400 error.
        //Remove it out for now.
        idSet.remove(6999);
        return idSet;
    }

    /**
     * Get a Coin Market Cap listing, which contains the Ids mapped to the Coin Market Cap data for each coin.
     *
     * @param visitor the exchange visitor used to use an alternate symbol to
     *                match what the coin market cap is expecting.
     * @return the listing, which contains the map.
     */
    public CoinMarketCapListing getCoinMarketCapListing(ExchangeVisitor visitor) {
        Set<Integer> idSet = getIdSet(visitor);
        return getCoinMarketCapListing(idSet);
    }

    /**
     * Get a Coin Market Cap listing for a set of coins.
     *
     * @param coinSet the set of coins to use.
     * @param visitor the exchange visitor used to use an alternate symbol to
     *                match what the coin market cap is expecting.
     * @return the listing, which contains the map.
     */
    public CoinMarketCapListing getCoinMarketCapListingWithCoinSet(Set<String> coinSet, ExchangeVisitor visitor) {
        Set<Integer> idSet = getIdSet(coinSet, visitor);
        CoinMarketCapListing listing = getCoinMarketCapListing(idSet);
        return listing;
    }

    /**
     * Set the market cap data for a list of coins.
     *
     * @param data the list of coins that will have the market cap data set.
     */
    public void setMarketCapDataFor24HrData(ExchangeVisitor visitor, List<CoinDataFor24Hr> data) {
        CoinMarketCapListing coinMarketCap = getCoinMarketCapListing(visitor);
        //If the coin market cap data exists, then update each symbol with the market cap value found in the market cap data.
        if (coinMarketCap != null) {
            data.forEach(d -> d.addMarketCapData(visitor, coinMarketCap));
        }
    }

    /**
     * Set the market cap data for a coin.
     *
     * @param coin the coin that will have the market cap data set.
     */
    public void setMarketCapDataFor24HrData(ExchangeVisitor visitor, CoinDataFor24Hr coin) {
        setMarketCapDataFor24HrData(visitor, Collections.singletonList(coin));
    }

    /**
     * Make the call to retrieve the coin market cap listing.
     *
     * @param idSet       the set of Ids that will be used to retrieve the data.
     * @param isForQuotes true if calling for exchange quotes, false if calling simply for exchange info.
     * @return the coin market cap listing.
     */
    private CoinMarketCapListing callCoinMarketCapListing(Set<Integer> idSet, boolean isForQuotes) {
        Supplier<CoinMarketCapListing> marketCapSupplier = () -> {
            List<NameValuePair> parameters = new ArrayList<>();

            //convert ids to comma separated String
            String value = idSet.stream().map(String::valueOf).collect(Collectors.joining(","));
            parameters.add(new BasicNameValuePair("id", value));

            String json;
            if (isForQuotes) {
                json = coinMarketCapApiService.makeExchangeQuotesApiCall(parameters);
            } else {
                json = coinMarketCapApiService.makeInfoApiCall(parameters);
            }
            List<CoinMarketCapData> data = parseJsonData(json, idSet);
            CoinMarketCapListing coinMarketCapListing = new CoinMarketCapListing();
            coinMarketCapListing = coinMarketCapListing.convertToCoinMarketCapListing(data);
            return coinMarketCapListing;
        };
        CoinMarketCapListing coinMarketCap;
        if (isForQuotes) {
            coinMarketCap = cacheUtil.retrieveFromCache(COIN_MARKET_CAP, LISTING, marketCapSupplier);
        } else {
            coinMarketCap = cacheUtil.retrieveFromCache(COIN_MARKET_CAP, INFO, marketCapSupplier);
        }
        return coinMarketCap;
    }

    /**
     * Get the coin market cap data/quotes listing based on the ids passed in.
     * This will retrieve it from the cache if it is in there. If not in the cache, the coin market cap api
     * will be called to supply the data.
     *
     * @param idSet a set of ids.
     * @return the market cap data for the coins.
     */
    public CoinMarketCapListing getCoinMarketCapListing(Set<Integer> idSet) {
        return callCoinMarketCapListing(idSet, true);
    }

    /**
     * Get the coin market cap info listing based on the ids passed in.
     * This will retrieve it from the cache if it is in there. If not in the cache, the coin market cap api
     * will be called to supply the data.
     *
     * @param idSet a set of ids.
     * @return the market cap data for the coins.
     */
    public CoinMarketCapListing getCoinMarketCapInfoListing(Set<Integer> idSet) {
        return callCoinMarketCapListing(idSet, false);
    }

    private Optional<JsonNode> parseJson(String json) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode;
        try {
            jsonNode = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            Log.error("Cannot parse json from CMC api call: {}", e.getMessage());
            return Optional.empty();
        }
        JsonNode data = jsonNode.get("data");
        return Optional.ofNullable(data);
    }

    private List<CoinMarketCapData> parseJsonData(String json, Set<Integer> idSet) {
        Optional<JsonNode> jsonNode = parseJson(json);
        if (jsonNode.isEmpty()) {
            return new ArrayList<>();
        }
        JsonNode data = jsonNode.get();
        List<CoinMarketCapData> list = new ArrayList<>();

        idSet.forEach(idNum -> {
            JsonNode node = data.get(String.valueOf(idNum));
            JsonNode idNode = node.get("id");
            int id = idNode.asInt();

            JsonNode nameNode = node.get("name");
            String name = nameNode.textValue();

            String logo = null;
            JsonNode logoNode = node.get("logo");
            if (logoNode != null) {
                logo = logoNode.textValue();
            }

            JsonNode symbolNode = node.get("symbol");
            String symbol = symbolNode.textValue();

            JsonNode dateAddedNode = node.get("date_added");
            String dateAdded = dateAddedNode.textValue();

            JsonNode quoteNode = node.get("quote");
            double volume24HrUsd = 0.0;
            double marketCap = 0.0;
            if (quoteNode != null) {
                JsonNode usdNode = quoteNode.get("USD");
                if (usdNode != null) {
                    JsonNode marketCapNode = usdNode.get("market_cap");
                    if (marketCapNode != null) {
                        marketCap = marketCapNode.asDouble();
                    }
                    JsonNode volume24HrNode = usdNode.get("volume_24h");
                    if (volume24HrNode != null) {
                        volume24HrUsd = volume24HrNode.asDouble();
                    }
                }
            }

            CoinMarketCapData d = new CoinMarketCapData();
            d.setId(id);
            d.setName(name);
            d.setSymbol(symbol);
            d.setMarketCap(marketCap);
            d.setVolume24HrUsd(volume24HrUsd);
            d.setLogo(logo);
            d.setDateAdded(dateAdded);
            list.add(d);
        });
        return list;
    }
}
