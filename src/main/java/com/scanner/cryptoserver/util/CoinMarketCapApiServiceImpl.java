package com.scanner.cryptoserver.util;

import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapMap;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class to encapsulate API http calls.
 * Some of these calls are more detailed than normal Spring
 * RestOperations, so use the examples in the api documentation.
 */
@Service(value = "coinMarketApiCapService")
public class CoinMarketCapApiServiceImpl implements CoinMarketCapApiService {
    private static final Logger Log = LoggerFactory.getLogger(CoinMarketCapApiServiceImpl.class);
    //todo: put this in a safe property
    @Value("${exchanges.coinmarketcap.map}")
    private String exchangeMapUrl;
    @Value("${exchanges.coinmarketcap.info}")
    private String exchangeInfoUrl;
    @Value("${exchanges.coinmarketcap.quotes}")
    private String exchangeQuotesUrl;
    private final RestOperations restTemplate;

    public CoinMarketCapApiServiceImpl(RestOperations restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public CoinMarketCapMap getCoinMarketCapMap() {
        Map<String, List<String>> params = new HashMap<>();

        //here, we add parameters to bring back only what we want
        params.put("aux", Arrays.asList("id", "name", "symbol"));
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-CMC_PRO_API_KEY", getKey());
        org.springframework.http.HttpEntity<CoinMarketCapMap> requestEntity = new org.springframework.http.HttpEntity<>(null, headers);
        Log.info("Calling coin market cap map: {}", exchangeMapUrl);
        //this api call is simple enough that a normal Spring Rest Operations call can be made, rather than the more complex Http calls
        ResponseEntity<CoinMarketCapMap> result = restTemplate.exchange(exchangeMapUrl, HttpMethod.GET, requestEntity, CoinMarketCapMap.class, params);
        return result.getBody();
    }

    @Override
    public String makeExchangeInfoApiCall(List<NameValuePair> paratmers) {
        try {
            return makeAPICall(exchangeInfoUrl, paratmers);
        } catch (URISyntaxException | IOException e) {
            Log.error("Cannot make coin market cap exchange info api call: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public String makeExchangeQuotesApiCall(List<NameValuePair> parameters) {
        try {
            return makeAPICall(exchangeQuotesUrl, parameters);
        } catch (URISyntaxException | IOException e) {
            Log.error("Cannot make coin market cap exchange quotes api call: {}", e.getMessage());
        }
        return null;
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
        request.addHeader("X-CMC_PRO_API_KEY", getKey());

        try (CloseableHttpResponse response = client.execute(request)) {
            HttpEntity entity = response.getEntity();
            responseContent = EntityUtils.toString(entity);
            EntityUtils.consume(entity);
        }

        return responseContent;
    }

    private String getKey() {
        return System.getenv("CoinMarketCapKey");
    }
}
