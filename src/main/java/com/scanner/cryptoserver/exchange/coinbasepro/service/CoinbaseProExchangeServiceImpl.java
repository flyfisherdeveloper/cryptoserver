package com.scanner.cryptoserver.exchange.coinbasepro.service;

import com.scanner.cryptoserver.exchange.AbstractExchangeService;
import com.scanner.cryptoserver.exchange.ExchangeService;
import com.scanner.cryptoserver.exchange.binance.us.dto.CoinDataFor24Hr;
import com.scanner.cryptoserver.exchange.binance.us.dto.CoinTicker;
import com.scanner.cryptoserver.exchange.binance.us.dto.CoinbaseProSymbol;
import com.scanner.cryptoserver.exchange.binance.us.dto.ExchangeInfo;
import com.scanner.cryptoserver.exchange.coinbasepro.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestOperations;

import java.time.Instant;
import java.util.List;

import static org.springframework.http.HttpMethod.GET;

@Service("CoinbasePro")
public class CoinbaseProExchangeServiceImpl extends AbstractExchangeService implements CoinbaseProExchangeService {
    private static final Logger Log = LoggerFactory.getLogger(CoinbaseProExchangeServiceImpl.class);

    private final Signature signature;
    private final ExchangeService exchangeAdapter;
    @Value("${exchanges.coinbasepro.info}")
    private String exchangeInfoUrl;
    private CoinbaseProSymbol[] coinbaseProExchangeInfo;

    public CoinbaseProExchangeServiceImpl(Signature signature, CacheManager cacheManager, RestOperations restTemplate) {
        super(cacheManager, restTemplate);
        this.signature = signature;
        this.exchangeAdapter = new CoinbaseProExchangeServiceAdapter(this);
    }

    public ExchangeInfo getExchangeInfo() {
        try {
            ResponseEntity<CoinbaseProSymbol[]> responseEntity = restTemplate.exchange(exchangeInfoUrl, GET,
                    //todo: fix this
                    securityHeaders("products", "GET", ""),
                    CoinbaseProSymbol[].class);
            coinbaseProExchangeInfo = responseEntity.getBody();
            return exchangeAdapter.getExchangeInfo();
        } catch (HttpClientErrorException ex) {
            Log.error("GET request Failed for '{}': exception: {}", exchangeInfoUrl, ex.getResponseBodyAsString());
        }
        return null;
    }

    @Override
    public List<CoinTicker> getTickerData(String symbol, String interval, String daysOrMonths) {
        return super.getTickerData("coinbaseProCoinCache", symbol, interval, daysOrMonths);
    }

    @Override
    protected List<CoinTicker> callCoinTicker(String symbol, String interval, String daysOrMonths) {
        return null;
    }

    @Override
    public CoinDataFor24Hr call24HrCoinTicker(String symbol) {
        return null;
    }

    @Override
    public List<CoinDataFor24Hr> get24HrAllCoinTicker() {
        return null;
    }

    @Override
    public byte[] getIconBytes(String coin) {
        return new byte[0];
    }

    @Override
    public List<CoinTicker> getCoinTicker(String ltcbtc, String s) {
        return null;
    }

    @Override
    public void add24HrVolumeChange(List<CoinDataFor24Hr> data) {

    }

    private HttpEntity<String> securityHeaders(String endpoint, String method, String jsonBody) {
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

    @Override
    public CoinbaseProSymbol[] getCoinbaseProExchangeInfo() {
        return coinbaseProExchangeInfo;
    }
}
