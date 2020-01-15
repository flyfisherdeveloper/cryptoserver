package com.scanner.cryptoserver.exchange.coinbasepro;

import com.scanner.cryptoserver.exchange.binance.us.dto.CoinbaseProSymbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestOperations;

import java.time.Instant;

import static org.springframework.http.HttpMethod.GET;

@Service
public class CoinbaseProExchangeService {
    private static final Logger Log = LoggerFactory.getLogger(CoinbaseProExchangeService.class);

    private final Signature signature;
    private final RestOperations restTemplate;
    @Value("${exchanges.coinbasepro.info}")
    private String exchangeInfoUrl;

    public CoinbaseProExchangeService(Signature signature, RestOperations restTemplate) {
        this.signature = signature;
        this.restTemplate = restTemplate;
    }

    public CoinbaseProSymbol[] getExchangeInfo() {
        try {
            ResponseEntity<CoinbaseProSymbol[]> responseEntity = restTemplate.exchange(exchangeInfoUrl, GET,
                    //todo: fix this
                    securityHeaders("products", "GET", ""),
                    CoinbaseProSymbol[].class);
            return responseEntity.getBody();
        } catch (HttpClientErrorException ex) {
            Log.error("GET request Failed for '" + exchangeInfoUrl + "': " + ex.getResponseBodyAsString());
        }
        return null;
    }

    public HttpEntity<String> securityHeaders(String endpoint, String method, String jsonBody) {
        HttpHeaders headers = new HttpHeaders();
        String publicKey = System.getenv("CoinbasePublicKey");
        String passPhrase = System.getenv("CoinbasePassPhrase");

        String timestamp = Instant.now().getEpochSecond() + "";
        String resource = endpoint.replace(exchangeInfoUrl, "");

        headers.add("accept", "application/json");
        headers.add("content-type", "application/json");
        headers.add("User-Agent", "java coinbase pro api library");
        headers.add("CB-ACCESS-KEY", publicKey);
        headers.add("CB-ACCESS-SIGN", signature.generate(resource, method, jsonBody, timestamp));
        headers.add("CB-ACCESS-TIMESTAMP", timestamp);
        headers.add("CB-ACCESS-PASSPHRASE", passPhrase);

        return new HttpEntity<>(jsonBody, headers);
    }
}
