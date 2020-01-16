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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.springframework.http.HttpMethod.GET;

@Service(value = "coinbaseProService")
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

    private ResponseEntity<?> callRestGet(String endpoint, Class<?> theClass) {
        try {
            return restTemplate.exchange(exchangeInfoUrl, GET, securityHeaders(endpoint, "GET", ""), theClass);
        } catch (HttpClientErrorException ex) {
            Log.error("GET request Failed for '{}': exception: {}", exchangeInfoUrl, ex.getResponseBodyAsString());
        }
        return null;
    }

    public ExchangeInfo getExchangeInfo() {
        ResponseEntity<CoinbaseProSymbol[]> products = (ResponseEntity<CoinbaseProSymbol[]>) callRestGet("products", CoinbaseProSymbol[].class);
        if (products == null) {
            return new ExchangeInfo();
        }
        coinbaseProExchangeInfo = products.getBody();
        return exchangeAdapter.getExchangeInfo();
    }

    @Override
    public List<CoinTicker> getTickerData(String symbol, String interval, String daysOrMonths) {
        return super.getTickerData("coinbaseProCoinCache", symbol, interval, daysOrMonths);
    }

    /*
    HTTP REQUEST
    GET /products/<product-id>/candles

            PARAMETERS
    Param	Description
    start	Start time in ISO 8601
    end	End time in ISO 8601
    granularity	Desired timeslice in seconds
    example: https://api.pro.coinbase.com/products/BTC-USD/candles?start=2018-07-10T12:00:00&stop=2018-07-15T12:00:00&granularity=900
     */
    @Override
    protected List<CoinTicker> callCoinTicker(String symbol, String interval, Long startTime, Long endTime) {
        //todo: what about 1 day?
        //todo: make work for coinbase pro: 24h, 6h, 1h, 15min, 5min, 1min
        String hoursStr = interval.substring(0, interval.length() - 1);
        String url = exchangeInfoUrl + "/" + symbol + "/candles?";

        String startTimeStr = "";
        String endTimeStr = "";
        if (startTime != null) {
            ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault());
            startTimeStr = zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
        if (endTime != null) {
            ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.systemDefault());
            endTimeStr = zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
        if (startTime == null || endTime == null) {
            return new ArrayList<>();
        }
        int hours = Integer.parseInt(hoursStr);
        int seconds = hours * 3600;
        String symbolTickerEnd = String.format("&start=%s&stop=%s&granularity=%s", startTimeStr, endTimeStr, seconds);
        url += symbolTickerEnd;

        ResponseEntity<Object[]> info = (ResponseEntity<Object[]>) callRestGet(url, Object[].class);
        if (info == null) {
            return new ArrayList<>();
        }
        Object[] body = info.getBody();
        if (body == null) {
            return new ArrayList<>();
        }

        List<CoinTicker> values = new ArrayList<>();
        for (Object obj : body) {
            LinkedHashMap<String, String> list = (LinkedHashMap<String, String>) obj;
            CoinTicker coinTicker = new CoinTicker();
            coinTicker.setSymbol(symbol);
            //todo: figure out open time
            coinTicker.setOpenTime((Long) list.get(0));
            coinTicker.setLow(Double.valueOf((String) list.get(1)));
            coinTicker.setHigh(Double.valueOf((String) list.get(2)));
            coinTicker.setOpen(Double.valueOf((String) list.get(3)));
            coinTicker.setClose(Double.valueOf((String) list.get(4)));
            coinTicker.setVolume(Double.valueOf((String) list.get(5)));
            //coinTicker.setCloseTime((Long) list.get(6));
            //coinTicker.setQuoteAssetVolume(Double.valueOf((String) list.get(7)));
            //coinTicker.setNumberOfTrades((int) list.get(8));
            values.add(coinTicker);
        }
        return values;
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
