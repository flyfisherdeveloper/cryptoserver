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
     * Get a set of IDs that represent coins. Use the exchange info from the Binance services to get the coin names.
     *
     * @return a Set of IDs.
     */
    public Set<Integer> getIdSet() {
        Set<String> coinSet = new HashSet<>();
        //get the exchange info for both Binance exchanges - this is to get a list of coins to retrieve the market cap for each coin
        cacheUtil.getExchangeNames().forEach(exchange -> {
            String name = exchange + "-" + EXCHANGE_INFO;
            ExchangeInfo exchangeInfo = cacheUtil.retrieveFromCache(EXCHANGE_INFO, name, null);
            if (exchangeInfo != null) {
                coinSet.addAll(exchangeInfo.getSymbols().stream().map(Symbol::getBaseAsset).collect(Collectors.toSet()));
            }
        });
        return getIdSet(coinSet);
    }

    public Set<Integer> getIdSet(Set<String> coinSet) {
        Supplier<CoinMarketCapListing> supplier = coinMarketCapApiService::getCoinMarketCapMap;
        CoinMarketCapListing coinMarketCap = cacheUtil.retrieveFromCache(COIN_MARKET_CAP, MARKET_CAP_MAP, supplier);

        long start = System.currentTimeMillis();
        //now get a set of ids for the coins in the exchanges
        Function<String, Integer> findCoinId = (coin) -> {
            CoinMarketCapData data = coinMarketCap.getData().get(coin);
            //99% of the time the coin is in the map based on its symbol.
            //If not, do a search in the map in the values to find it based on its name instead of symbol.
            if (data == null) {
                return coinMarketCap.getData().values().stream()
                        .filter(c -> c.isCoin(coin))
                        .map(CoinMarketCapData::getId).findFirst().orElse(1);
            }
            return data.getId();
        };
        Set<Integer> idSet = coinSet.stream().map(findCoinId).collect(Collectors.toSet());
        System.out.println("getIdSet() 2: " + (System.currentTimeMillis() - start));
        return idSet;
    }

    public CoinMarketCapListing getCoinMarketCapListing() {
        Set<Integer> idSet = getIdSet();
        return getCoinMarketCapListing(idSet);
    }

    public CoinMarketCapListing getCoinMarketCapListingWithCoinSet(Set<String> coinSet) {
        Set<Integer> idSet = getIdSet(coinSet);
        CoinMarketCapListing listing = getCoinMarketCapListing(idSet);
        return listing;
    }

    public void setMarketCapFor24HrData(List<CoinDataFor24Hr> data) {
        CoinMarketCapListing coinMarketCap = getCoinMarketCapListing();
        //If the coin market cap data exists, then update each symbol with the market cap value found in the maket cap data.
        if (coinMarketCap != null) {
            data.forEach(d -> d.addMarketCap(coinMarketCap));
        }
    }

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

    public CoinMarketCapListing getCoinMarketCapInfo(Set<Integer> ids) {
        List<NameValuePair> paratmers = new ArrayList<>();
        String value = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        paratmers.add(new BasicNameValuePair("id", value));
        String json = coinMarketCapApiService.makeExchangeInfoApiCall(paratmers);

        List<CoinMarketCapData> data = parseJsonInfo(json, ids);
        CoinMarketCapListing listing = new CoinMarketCapListing();
        listing = listing.convertToCoinMarketCapListing(data);
        return listing;
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

            CoinMarketCapData d = new CoinMarketCapData();
            d.setId(id);
            d.setName(name);
            d.setSymbol(symbol);
            d.setMarketCap(marketCap);
            list.add(d);
        });
        return list;
    }

    private List<CoinMarketCapData> parseJsonInfo(String json, Set<Integer> ids) {
        Optional<JsonNode> jsonNode = parseJson(json);
        if (!jsonNode.isPresent()) {
            return new ArrayList<>();
        }
        JsonNode data = jsonNode.get();
        List<CoinMarketCapData> list = new ArrayList<>();

        for (int id : ids) {
            JsonNode idNode = data.get(String.valueOf(id));
            CoinMarketCapData d = new CoinMarketCapData();
            d.setId(id);
            d.setName(idNode.get("name").textValue());
            d.setSymbol(idNode.get("symbol").textValue());
            d.setDescription(idNode.get("description").textValue());
            d.setLogo(idNode.get("logo").textValue());
            list.add(d);
        }
        return list;
    }
}
