package com.scanner.cryptoserver.exchange.coinmarketcap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapData;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapMap;
import com.scanner.cryptoserver.util.CacheUtil;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class CoinMarketCapService {
    private static final Logger Log = LoggerFactory.getLogger(CoinMarketCapData.class);
    private static final String MARKET_CAP = "MarketCap";
    private static final String EXCHANGE_INFO = "ExchangeInfo";

    @Value("${exchanges.coinmarketcap.map}")
    private String exchangeMapUrl;
    @Value("${exchanges.coinmarketcap.info}")
    private String exchangeInfoUrl;
    @Value("${exchanges.coinmarketcap.listing}")
    private String exchangeListingUrl;
    //todo: put this in a safe property
    private String key = "f956ce89-7be3-4542-80c0-50e4a774e123";
    private final RestOperations restTemplate;
    private final CacheManager cacheManager;

    public CoinMarketCapService(RestOperations restTemplate, CacheManager cacheManager) {
        this.restTemplate = restTemplate;
        this.cacheManager = cacheManager;
    }

    public CoinMarketCapMap getCoinMarketCapMap() {
        Map<String, List<String>> params = new HashMap<>();

        //here, we add parameters to bring back only what we want
        params.put("aux", Arrays.asList("id", "name", "symbol"));
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-CMC_PRO_API_KEY", key);
        org.springframework.http.HttpEntity<CoinMarketCapMap> requestEntity = new org.springframework.http.HttpEntity<>(null, headers);
        Log.info("Calling coin market cap map: {}", exchangeMapUrl);
        ResponseEntity<CoinMarketCapMap> result = restTemplate.exchange(exchangeMapUrl, HttpMethod.GET, requestEntity, CoinMarketCapMap.class, params);
        return result.getBody();
    }

    public CoinMarketCapMap getCoinMarketCapListing() {
        Supplier<CoinMarketCapMap> marketCapSupplier = () -> {
            List<NameValuePair> paratmers = new ArrayList<NameValuePair>();
            paratmers.add(new BasicNameValuePair("start", "1"));
            paratmers.add(new BasicNameValuePair("limit", "399"));

            CoinMarketCapMap coinMarketCapMap = new CoinMarketCapMap();
            try {
                String json = makeAPICall(exchangeListingUrl, paratmers);
                List<CoinMarketCapData> data = parseJsonData(json);
                coinMarketCapMap.setData(data);
                return coinMarketCapMap;
            } catch (IOException | URISyntaxException e) {
                Log.error("Coin Market Cap Listing api call failed: {}", e.getMessage());
                coinMarketCapMap.setData(new ArrayList<>());
                return coinMarketCapMap;
            }
        };
        CoinMarketCapMap coinMarketCap = CacheUtil.retrieveFromCache(cacheManager, EXCHANGE_INFO, MARKET_CAP, marketCapSupplier);
        return coinMarketCap;
    }

    public CoinMarketCapMap getCoinMarketCapInfo(List<Integer> ids) {
        List<NameValuePair> paratmers = new ArrayList<>();
        String value = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        paratmers.add(new BasicNameValuePair("id", value));
        String json = "";
        try {
            json = makeAPICall(exchangeInfoUrl, paratmers);
        } catch (URISyntaxException | IOException e) {
            Log.error("Coin Market Cap Info api call failed: {}", e.getMessage());
        }

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

    private List<CoinMarketCapData> parseJsonData(String json) {
        Optional<JsonNode> jsonNode = parseJson(json);
        if (!jsonNode.isPresent()) {
            return new ArrayList<>();
        }
        JsonNode data = jsonNode.get();
        List<CoinMarketCapData> list = new ArrayList<>();

        for (int num = 0; num < data.size(); num++) {
            JsonNode node = data.get(num);
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
        }
        return list;
    }

    private List<CoinMarketCapData> parseJsonInfo(String json, List<Integer> ids) {
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

    /**
     * Make a CMC call. The code here is taken from the api documentation (with a few minor modifications for clarity).
     * see: https://sandbox.coinmarketcap.com/api/v1/#operation/getV1CryptocurrencyInfo
     *
     * @param uri        the address of the api call.
     * @param parameters name/value pairs that are parameters of the call, such as a list of coin ids.
     * @return the json result.
     * @throws URISyntaxException on a bad address.
     * @throws IOException        an IO exception.
     */
    private String makeAPICall(String uri, List<NameValuePair> parameters)
            throws URISyntaxException, IOException {
        String responseContent;
        URIBuilder query = new URIBuilder(uri);
        query.addParameters(parameters);

        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet request = new HttpGet(query.build());
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        request.addHeader("X-CMC_PRO_API_KEY", key);

        try (CloseableHttpResponse response = client.execute(request)) {
            HttpEntity entity = response.getEntity();
            responseContent = EntityUtils.toString(entity);
            EntityUtils.consume(entity);
        }

        return responseContent;
    }
}
