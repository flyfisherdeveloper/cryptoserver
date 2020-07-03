package com.scanner.cryptoserver.exchange.coinmarketcap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scanner.cryptoserver.exchange.binance.dto.ExchangeInfo;
import com.scanner.cryptoserver.exchange.binance.dto.Symbol;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapData;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapMap;
import com.scanner.cryptoserver.util.CacheUtil;
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
        Supplier<CoinMarketCapMap> supplier = coinMarketCapApiService::getCoinMarketCapMap;
        CoinMarketCapMap coinMarketCap = cacheUtil.retrieveFromCache(COIN_MARKET_CAP, MARKET_CAP_MAP, supplier);

        //now get a set of ids for the coins in the exchanges
        Function<String, Integer> findCoinId = (coin) -> coinMarketCap.getData()
                .stream()
                .filter(c -> c.isCoin(coin))
                .map(CoinMarketCapData::getId).findFirst().orElse(1);
        Set<Integer> idSet = coinSet.stream().map(findCoinId).collect(Collectors.toSet());
        return idSet;
    }

    public CoinMarketCapMap getCoinMarketCapListing() {
        Set<Integer> idSet = getIdSet();
        return getCoinMarketCapListing(idSet);
    }

    public CoinMarketCapMap getCoinMarketCapListing(Set<Integer> idSet) {
        Supplier<CoinMarketCapMap> marketCapSupplier = () -> {
            List<NameValuePair> parameters = new ArrayList<>();

            //convert ids to comma separated String
            String value = idSet.stream().map(String::valueOf).collect(Collectors.joining(","));
            parameters.add(new BasicNameValuePair("id", value));

            CoinMarketCapMap coinMarketCapMap = new CoinMarketCapMap();
            String json = coinMarketCapApiService.makeExchangeQuotesApiCall(parameters);
            List<CoinMarketCapData> data = parseJsonData(json, idSet);
            coinMarketCapMap.setData(data);
            return coinMarketCapMap;
        };
        CoinMarketCapMap coinMarketCap = cacheUtil.retrieveFromCache(COIN_MARKET_CAP, LISTING, marketCapSupplier);
        return coinMarketCap;
    }

    public CoinMarketCapMap getCoinMarketCapInfo(Set<Integer> ids) {
        List<NameValuePair> paratmers = new ArrayList<>();
        String value = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        paratmers.add(new BasicNameValuePair("id", value));
        String json = coinMarketCapApiService.makeExchangeInfoApiCall(paratmers);

        List<CoinMarketCapData> data = parseJsonInfo(json, ids);
        CoinMarketCapMap map = new CoinMarketCapMap();
        map.setData(data);
        return map;
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
