package com.scanner.myscanner.exchange.binance.us.service;

import com.scanner.myscanner.exchange.binance.us.dto.CoinDataFor24Hr;
import com.scanner.myscanner.exchange.binance.us.dto.CoinTicker;
import com.scanner.myscanner.exchange.binance.us.dto.ExchangeInfo;
import com.scanner.myscanner.exchange.binance.us.dto.Symbol;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ExchangeService {
    private String exchangeInfoUrl = "https://api.binance.us/api/v3/exchangeInfo";
    private String symblolTickerTimeParams = "&startTime={startTime}&endTime={endTime}";
    private String symblolTickerUrl = "https://api.binance.us/api/v3/klines?symbol={symbol}&interval={interval}";
    private String symbol24HrTickerUrl = "https://api.binance.us/api/v3/ticker/24hr?symbol={symbol}";
    private final RestTemplate restTemplate;

    public ExchangeService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ExchangeInfo getExchangeInfo() {
        ResponseEntity<ExchangeInfo> info = restTemplate.getForEntity(exchangeInfoUrl, ExchangeInfo.class);
        return info.getBody();
    }

    public ExchangeInfo getMockExchangeInfo() {
        ExchangeInfo info = new ExchangeInfo();
        List<Symbol> symbols = new ArrayList<>();

        Symbol coin1 = new Symbol();
        coin1.setSymbol("BTCUSD");
        coin1.setBaseAsset("BTC");
        coin1.setQuoteAsset("USD");
        symbols.add(coin1);

        Symbol coin2 = new Symbol();
        coin2.setSymbol("ETHUSD");
        coin2.setBaseAsset("ETH");
        coin2.setQuoteAsset("USD");
        symbols.add(coin2);

        Symbol coin3 = new Symbol();
        coin3.setSymbol("XRPUSD");
        coin3.setBaseAsset("XRP");
        coin3.setQuoteAsset("USD");
        symbols.add(coin3);

        Symbol coin4 = new Symbol();
        coin4.setSymbol("LTCUSD");
        coin4.setBaseAsset("LTC");
        coin4.setQuoteAsset("USD");
        symbols.add(coin4);

        info.setSymbols(symbols);
        return info;
    }

    public List<CoinTicker> getCoinTicker(String symbol, String interval) {
        return callCoinTicker(symbol, interval, null, null);
    }

    public List<CoinTicker> getCoinTicker(String symbol, String interval, long startTime, long endTime) {
        return callCoinTicker(symbol, interval, startTime, endTime);
    }

    public List<CoinDataFor24Hr> getMock24HrCoinTicker() {
        List<CoinDataFor24Hr> list = new ArrayList<>();

        CoinDataFor24Hr coin1 = new CoinDataFor24Hr();
        coin1.setSymbol("LTCUSD");
        coin1.setPriceChange(-1.2);
        coin1.setPriceChangePercent(-2.023);
        coin1.setHighPrice(61.13);
        coin1.setLowPrice(57.04);
        coin1.setVolume(4050.19611);
        coin1.setQuoteVolume(239099.0);
        coin1.setOpenTime(1572376515329L);
        coin1.setCloseTime(1572462915329L);
        list.add(coin1);

        CoinDataFor24Hr coin2 = new CoinDataFor24Hr();
        coin2.setSymbol("BTCUSD");
        coin2.setPriceChange(206.1400);
        coin2.setPriceChangePercent(2.272);
        coin2.setHighPrice(9411.4300);
        coin2.setLowPrice(8955.1200);
        coin2.setVolume(322.83641200);
        coin2.setQuoteVolume(2952333.2810);
        coin2.setOpenTime(1572448433844L);
        coin2.setCloseTime(1572534833844L);
        list.add(coin2);

        CoinDataFor24Hr coin3 = new CoinDataFor24Hr();
        coin3.setSymbol("ETHUSD");
        coin3.setPriceChange(2.0100);
        coin3.setPriceChangePercent(1.109);
        coin3.setHighPrice(185.4200);
        coin3.setLowPrice(178.02);
        coin3.setVolume(1672.33941000);
        coin3.setQuoteVolume(304078.6850);
        coin3.setOpenTime(1572448416810L);
        coin3.setCloseTime(1572534816810L);
        list.add(coin3);

        return list;
    }

    public CoinDataFor24Hr get24HrCoinTicker(String symbol) {
        String url = symbol24HrTickerUrl;
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        ResponseEntity<LinkedHashMap> info = restTemplate.getForEntity(url, LinkedHashMap.class, params);
        LinkedHashMap body = info.getBody();
        if (body == null) {
            return new CoinDataFor24Hr();
        }
        CoinDataFor24Hr data = new CoinDataFor24Hr();

        String coin = (String) body.get("symbol");
        data.setSymbol(coin);

        String priceChangeStr = (String) body.get("priceChange");
        double priceChange = Double.parseDouble(priceChangeStr);
        data.setPriceChange(priceChange);

        String priceChangePercentStr = (String) body.get("priceChangePercent");
        double priceChangePercent = Double.parseDouble(priceChangePercentStr);
        data.setPriceChangePercent(priceChangePercent);

        String highPriceStr = (String) body.get("highPrice");
        double highPrice = Double.parseDouble(highPriceStr);
        data.setHighPrice(highPrice);

        String lowPriceStr = (String) body.get("lowPrice");
        double lowPrice = Double.parseDouble(lowPriceStr);
        data.setLowPrice(lowPrice);

        String volumeStr = (String) body.get("volume");
        double volume = Double.parseDouble(volumeStr);
        data.setVolume(volume);

        String quoteVolumeStr = (String) body.get("quoteVolume");
        double quoteVolume = Double.parseDouble(quoteVolumeStr);
        data.setQuoteVolume(quoteVolume);

        Long openTime = (Long) body.get("openTime");
        data.setOpenTime(openTime);

        Long closeTime = (Long) body.get("closeTime");
        data.setCloseTime(closeTime);

        return data;
    }

    private List<CoinTicker> callCoinTicker(String symbol, String interval, Long startTime, Long endTime) {
        //todo: check that both start time and end time are either both null or both not null
        String url = symblolTickerUrl;
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("interval", interval);
        if (startTime != null) {
            params.put("startTime", startTime);
            url += symblolTickerTimeParams;
        }
        if (endTime != null) {
            params.put("endTime", endTime);
        }
        ResponseEntity<Object[]> info = restTemplate.getForEntity(url, Object[].class, params);
        Object[] body = info.getBody();
        if (body == null) {
            return new ArrayList<>();
        }

        List<CoinTicker> values = new ArrayList<>();
        for (Object obj : body) {
            List<Object> list = (List<Object>) obj;
            CoinTicker coinTicker = new CoinTicker();
            coinTicker.setOpenTime((Long) list.get(0));
            coinTicker.setOpen((String) list.get(1));
            coinTicker.setHigh((String) list.get(2));
            coinTicker.setLow((String) list.get(3));
            coinTicker.setClose((String) list.get(4));
            coinTicker.setVolume((String) list.get(5));
            coinTicker.setCloseTime((Long) list.get(6));
            coinTicker.setQuoteAssetVolume((String) list.get(7));
            coinTicker.setNumberOfTrades((int) list.get(8));
            values.add(coinTicker);
        }
        return values;
    }

    public List<CoinTicker> get7DayTicker(String symbol) {
        Instant now = Instant.now();
        Instant from = now.minus(7, ChronoUnit.DAYS);
        long startTime = from.toEpochMilli();
        long toTime = now.toEpochMilli();
        return callCoinTicker(symbol, "12h", startTime, toTime);
    }

    public List<CoinTicker> getMock7DayTicker(String symbol) {
        List<CoinTicker> list = new ArrayList<>();
        CoinTicker coin1 = new CoinTicker();
        coin1.setOpenTime(1571961600000L);
        coin1.setCloseTime(1572004799999L);
        coin1.setVolume("268404.00000000");
        coin1.setQuoteAssetVolume("671.50380360");
        list.add(coin1);

        CoinTicker coin2 = new CoinTicker();
        coin2.setOpenTime(1572220800000L);
        coin2.setCloseTime(1572263999999L);
        coin2.setVolume("140122.00000000");
        coin2.setQuoteAssetVolume("373.25104520");
        list.add(coin2);

        CoinTicker coin3 = new CoinTicker();
        coin3.setOpenTime(1572523200000L);
        coin3.setCloseTime(1572566399999L);
        coin3.setVolume("292904.00000000");
        coin3.setQuoteAssetVolume("763.28951510");
        list.add(coin3);

        return list;
    }
}
