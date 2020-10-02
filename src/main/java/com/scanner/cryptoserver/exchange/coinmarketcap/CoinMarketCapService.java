package com.scanner.cryptoserver.exchange.coinmarketcap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapData;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapListing;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.ExchangeInfo;
import com.scanner.cryptoserver.util.CacheUtil;
import com.scanner.cryptoserver.util.dto.Symbol;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
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
     * @return a Set of IDs.
     */
    public Set<Integer> getIdSet() {
        Set<String> coinSet = new HashSet<>();
        //get the exchange info for all exchanges - this is to get a list of coins to retrieve the market cap for each coin
        cacheUtil.getExchangeNames().forEach(exchangeName -> {
            String name = exchangeName + "-" + EXCHANGE_INFO;
            ExchangeInfo exchangeInfo = cacheUtil.retrieveExchangeInfoFromCache(exchangeName, EXCHANGE_INFO, name);
            if (exchangeInfo != null) {
                coinSet.addAll(exchangeInfo.getSymbols().stream().map(Symbol::getBaseAsset).collect(Collectors.toSet()));
            }
        });
        return getIdSet(coinSet);
    }

    /**
     * Get a set of Coin Market Cap Ids for a set of coins.
     *
     * @param coinSet the coin set that will be used to look up the Ids.
     * @return the set of Ids, based on the Coin Market Cap service.
     */
    public Set<Integer> getIdSet(Set<String> coinSet) {
        Supplier<CoinMarketCapListing> supplier = coinMarketCapApiService::getCoinMarketCapMap;
        CoinMarketCapListing coinMarketCap = cacheUtil.retrieveFromCache(COIN_MARKET_CAP, MARKET_CAP_MAP, supplier);

        //now get a set of ids for the coins in the exchanges
        Function<String, Integer> findCoinId = (coin) -> coinMarketCap.getData().values().stream()
                .filter(c -> c.isCoin(coin))
                .map(CoinMarketCapData::getId).findFirst().orElse(1);
        Set<Integer> idSet = coinSet.stream().map(findCoinId).collect(Collectors.toSet());
        //Note. Id of 6999 is causing the coin market cap API to return a 400 error.
        //Remove it out for now.
        idSet.remove(6999);
        return idSet;
    }

    /**
     * Get a Coin Market Cap listing, which contains the Ids mapped to the Coin Market Cap data for each coin.
     *
     * @return the listing, which contains the map.
     */
    public CoinMarketCapListing getCoinMarketCapListing() {
        Set<Integer> idSet = getIdSet();
        return getCoinMarketCapListing(idSet);
    }

    /**
     * Get a Coin Market Cap listing for a set of coins.
     *
     * @param coinSet the set of coins to use.
     * @return the listing, which contains the map.
     */
    public CoinMarketCapListing getCoinMarketCapListingWithCoinSet(Set<String> coinSet) {
        Set<Integer> idSet = getIdSet(coinSet);
        CoinMarketCapListing listing = getCoinMarketCapListing(idSet);
        return listing;
    }

    /**
     * Set the market cap data for a list of coins.
     *
     * @param data the list of coins that will have the market cap data set.
     */
    public void setMarketCapDataFor24HrData(List<CoinDataFor24Hr> data) {
        CoinMarketCapListing coinMarketCap = getCoinMarketCapListing();
        //If the coin market cap data exists, then update each symbol with the market cap value found in the maket cap data.
        if (coinMarketCap != null) {
            data.forEach(d -> d.addMarketCapData(coinMarketCap));
        }
    }

    /**
     * Set the market cap data for a coin.
     *
     * @param coin the coin that will have the market cap data set.
     */
    public void setMarketCapDataFor24HrData(CoinDataFor24Hr coin) {
        setMarketCapDataFor24HrData(Collections.singletonList(coin));
    }

    /**
     * Get the coin market cap listing based on the ids passed in.
     * This will retrieve it from the cache if it is in there. If not in the cache, the coin market cap api
     * will be called to supply the data.
     *
     * @param idSet a set of ids.
     * @return the market cap data for the coins.
     */
    public CoinMarketCapListing getCoinMarketCapListing(Set<Integer> idSet) {
        Supplier<CoinMarketCapListing> marketCapSupplier = () -> {
            List<NameValuePair> parameters = new ArrayList<>();

            //convert ids to comma separated String
            String value = idSet.stream().map(String::valueOf).collect(Collectors.joining(","));
            parameters.add(new BasicNameValuePair("id", value));

            String json = coinMarketCapApiService.makeExchangeQuotesApiCall(parameters);
            List<CoinMarketCapData> data = parseJsonData(json, idSet);
            CoinMarketCapListing coinMarketCapListing = new CoinMarketCapListing();
            coinMarketCapListing = coinMarketCapListing.convertToCoinMarketCapListing(data);
            return coinMarketCapListing;
        };
        CoinMarketCapListing coinMarketCap = cacheUtil.retrieveFromCache(COIN_MARKET_CAP, LISTING, marketCapSupplier);
        return coinMarketCap;
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
        if (!jsonNode.isPresent()) {
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
            JsonNode symbolNode = node.get("symbol");
            String symbol = symbolNode.textValue();
            JsonNode quoteNode = node.get("quote");
            JsonNode usdNode = quoteNode.get("USD");
            JsonNode marketCapNode = usdNode.get("market_cap");
            double marketCap = marketCapNode.asDouble();
            JsonNode volume24HrNode = usdNode.get("volume_24h");
            double volume24HrUsd = volume24HrNode.asDouble();

            CoinMarketCapData d = new CoinMarketCapData();
            d.setId(id);
            d.setName(name);
            d.setSymbol(symbol);
            d.setMarketCap(marketCap);
            d.setVolume24HrUsd(volume24HrUsd);
            list.add(d);
        });
        return list;
    }
}
