package com.scanner.myscanner.exchange.binance.us.service;

import com.scanner.myscanner.exchange.binance.us.dto.CoinDataFor24Hr;
import com.scanner.myscanner.exchange.binance.us.dto.CoinTicker;
import com.scanner.myscanner.exchange.binance.us.dto.ExchangeInfo;
import com.scanner.myscanner.exchange.binance.us.dto.Symbol;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;
import util.IconExtractor;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExchangeService {
    private String exchangeInfoUrl = "https://api.binance.us/api/v3/exchangeInfo";
    private String symblolTickerTimeParams = "&startTime={startTime}&endTime={endTime}";
    private String symblolTickerUrl = "https://api.binance.us/api/v3/klines?symbol={symbol}&interval={interval}";
    private String symbol24HrTickerUrl = "https://api.binance.us/api/v3/ticker/24hr?symbol={symbol}";
    private String symbol24HrAllTickerUrl = "https://api.binance.us/api/v3/ticker/24hr";
    private final RestOperations restTemplate;
    private final CacheManager cacheManager;

    public ExchangeService(RestOperations restTemplate, CacheManager cacheManager) {
        this.restTemplate = restTemplate;
        this.cacheManager = cacheManager;
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
        coin1.setCoin("LTC");
        coin1.setCurrency("USD");
        coin1.setLastPrice(56.23);
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
        coin2.setCoin("BTC");
        coin2.setCurrency("USD");
        coin2.setLastPrice(8243.32);
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
        coin3.setCoin("ETH");
        coin3.setCurrency("USD");
        coin3.setLastPrice(124.20);
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

    public CoinDataFor24Hr call24HrCoinTicker(String symbol) {
        String url = symbol24HrTickerUrl;
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        ResponseEntity<LinkedHashMap> info = restTemplate.getForEntity(url, LinkedHashMap.class, params);
        LinkedHashMap body = info.getBody();
        if (body == null) {
            return new CoinDataFor24Hr();
        }
        return get24HrCoinTicker(body);
    }

    private int getQuoteOffset(String symbol) {
        var offset = 3;
        if (symbol.endsWith("USDT")) {
            offset = 4;
        }
        return offset;
    }

    private int getStartOfQuote(String str) {
        var end = str.length();
        var offset = this.getQuoteOffset(str);
        return end - offset;
    }

    private String getQuote(String str) {
        var start = this.getStartOfQuote(str);
        return str.substring(start);
    }

    private String getCoin(String str) {
        var offset = this.getStartOfQuote(str);
        return str.substring(0, offset);
    }

    private CoinDataFor24Hr get24HrCoinTicker(LinkedHashMap map) {
        CoinDataFor24Hr data = new CoinDataFor24Hr();
        String symbol = (String) map.get("symbol");
        String coin = getCoin(symbol);
        String currency = getQuote(symbol);

        data.setSymbol(symbol);
        data.setCoin(coin);
        data.setCurrency(currency);
        String priceChangeStr = (String) map.get("priceChange");
        double priceChange = Double.parseDouble(priceChangeStr);
        data.setPriceChange(priceChange);

        String priceChangePercentStr = (String) map.get("priceChangePercent");
        double priceChangePercent = Double.parseDouble(priceChangePercentStr);
        data.setPriceChangePercent(priceChangePercent);

        String lastPriceStr = (String) map.get("lastPrice");
        double lastPrice = Double.parseDouble(lastPriceStr);
        data.setLastPrice(lastPrice);

        String highPriceStr = (String) map.get("highPrice");
        double highPrice = Double.parseDouble(highPriceStr);
        data.setHighPrice(highPrice);

        String lowPriceStr = (String) map.get("lowPrice");
        double lowPrice = Double.parseDouble(lowPriceStr);
        data.setLowPrice(lowPrice);

        String volumeStr = (String) map.get("volume");
        double volume = Double.parseDouble(volumeStr);
        data.setVolume(volume);

        String quoteVolumeStr = (String) map.get("quoteVolume");
        double quoteVolume = Double.parseDouble(quoteVolumeStr);
        data.setQuoteVolume(quoteVolume);

        Long openTime = (Long) map.get("openTime");
        data.setOpenTime(openTime);

        Long closeTime = (Long) map.get("closeTime");
        data.setCloseTime(closeTime);

        data.setupLinks();

        return data;
    }

    private List<CoinTicker> callCoinTicker(String symbol, String interval, Long startTime, Long endTime) {
        //todo: check that both start time and end time are either both null or both not null
        if (interval.equals("24h")) {
            interval = "1d";
        }
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
            coinTicker.setSymbol(symbol);
            coinTicker.setOpenTime((Long) list.get(0));
            coinTicker.setOpen(Double.valueOf((String) list.get(1)));
            coinTicker.setHigh(Double.valueOf((String) list.get(2)));
            coinTicker.setLow(Double.valueOf((String) list.get(3)));
            coinTicker.setClose(Double.valueOf((String) list.get(4)));
            coinTicker.setVolume(Double.valueOf((String) list.get(5)));
            coinTicker.setCloseTime((Long) list.get(6));
            coinTicker.setQuoteAssetVolume(Double.valueOf((String) list.get(7)));
            coinTicker.setNumberOfTrades((int) list.get(8));
            values.add(coinTicker);
        }
        return values;
    }

    public List<CoinTicker> getDayTicker(String symbol, String interval, String daysOrMonths) {
        Instant now = Instant.now();
        Instant from;
        if (daysOrMonths.endsWith("d")) {
            int numDays = Integer.parseInt("" + daysOrMonths.charAt(0));
            from = now.minus(numDays, ChronoUnit.DAYS);
        } else {
            //todo: Instant does not support ChronoUnit.MONTHS - use 30 days as a workaround for now
            from = now.minus(30, ChronoUnit.DAYS);
        }
        long startTime = from.toEpochMilli();
        long toTime = now.toEpochMilli();

        //Attempt to get the data out of the cache if it is in there.
        //If not in the cache, then call the service and add the data to the cache.
        //The data in the cache will expire according to the setup in the CachingConfig configuration.
        Cache volumeCache = cacheManager.getCache("VolumeCache");
        String name = symbol + interval + daysOrMonths;
        List<CoinTicker> coins;
        if (volumeCache != null) {
            Cache.ValueWrapper value = volumeCache.get(name);
            if (value == null) {
                coins = callCoinTicker(symbol, interval, startTime, toTime);
                volumeCache.putIfAbsent(name, coins);
            } else {
                coins = (List<CoinTicker>) value.get();
            }
            return coins;
        }
        return new ArrayList<>();
    }

    public List<CoinTicker> getMock7DayTicker(String symbol) {
        List<CoinTicker> list = new ArrayList<>();
        CoinTicker coin1 = new CoinTicker();
        coin1.setOpenTime(1571961600000L);
        coin1.setCloseTime(1572004799999L);
        coin1.setCloseDate("12 OCT 2019");
        coin1.setVolume(268404.00);
        coin1.setQuoteAssetVolume(671.50380360);
        list.add(coin1);

        CoinTicker coin2 = new CoinTicker();
        coin2.setOpenTime(1572220800000L);
        coin2.setCloseTime(1572263999999L);
        coin2.setCloseDate("13 OCT 2019");
        coin2.setVolume(140122.00);
        coin2.setQuoteAssetVolume(373.25104520);
        list.add(coin2);

        CoinTicker coin3 = new CoinTicker();
        coin3.setOpenTime(1572523200000L);
        coin3.setCloseTime(1572566399999L);
        coin3.setCloseDate("14 OCT 2019");
        coin3.setVolume(292904.00);
        coin3.setQuoteAssetVolume(763.28951510);
        list.add(coin3);

        return list;
    }

    public Double getPercentChange(double fromValue, double toValue) {
        double change = toValue - fromValue;
        return (change / fromValue) * 100.0;
    }

    //Put the start and end times in an array - from time first, to time second.
    //Essentially this a "Tuple" or a "Pair", but is simple enough to just use a simple array of two longs.
    private long[] getStartAndEndTime(int minusHours, int minusMinutes) {
        Instant now = Instant.now();
        Instant to = now.minus(minusMinutes, ChronoUnit.MINUTES);
        Instant from = to.minus(minusHours, ChronoUnit.HOURS);
        long fromTime = from.toEpochMilli();
        long toTime = to.toEpochMilli();
        return new long[]{fromTime, toTime};
    }

    //todo: add unit tests
    public void add24HrVolumeChange(List<CoinDataFor24Hr> data) {
        List<String> coins = data.stream().map(CoinDataFor24Hr::getSymbol).collect(Collectors.toList());

        //jeff
        //todo: change to parallel stream
        //the api does not give volume percent change information for intervals
        //so... a workaround:
        //here, we go back 2 days and get volume info in 15-minute intervals
        //then, we calculate the total volume of day 1 and compare to day 2 to get a percent volume change
        //note: this is for volume change percentage in days; todo: modify for ANY interval, not just days
        coins.parallelStream().forEach(coin -> {
            long[] startAndEndTime = getStartAndEndTime(48, 15);
            //get all the data for 15-min intervals going back 2 days
            List<CoinTicker> coinTickers = callCoinTicker(coin, "15m", startAndEndTime[0], startAndEndTime[1]);
            //sort by close time
            coinTickers = coinTickers.stream().sorted(Comparator.comparingLong(CoinTicker::getCloseTime)).collect(Collectors.toList());
            //now compute the volumes for day 1 and day 2
            startAndEndTime = getStartAndEndTime(24, 0);
            long startTime = startAndEndTime[0];

            //todo: eliminate when debugging is complete
            /*
            long count1 = coinTickers.stream().filter(ticker -> ticker.getCloseTime() <= startTime).count();
            long count2 = coinTickers.stream().filter(ticker -> ticker.getCloseTime() > startTime).count();
            if (count1 != count2) {
                throw new RuntimeException("Different numbers of tickers for volume change calculation");
            }
             */

            /*
            System.out.println("start time: " + new Date(startTime));
            coinTickers.stream().filter(ticker -> ticker.getCloseTime() <= startTime).forEach(c -> System.out.println(new Date(c.getCloseTime())));
            System.out.println("----------------------------------------------");
            coinTickers.stream().filter(ticker -> ticker.getCloseTime() > startTime).forEach(c -> System.out.println(new Date(c.getCloseTime())));
             */
            //volume for day 1
            double prevDayVolume = coinTickers.stream().filter(ticker -> ticker.getCloseTime() <= startTime).map(CoinTicker::getQuoteAssetVolume).mapToDouble(vol -> vol).sum();
            //volume for day 2
            double newDayVolume = coinTickers.stream().filter(ticker -> ticker.getCloseTime() > startTime).map(CoinTicker::getQuoteAssetVolume).mapToDouble(vol -> vol).sum();
            //if volume is 0.0 then the data is missing (such as a brand new coin with only one day of data)
            if (prevDayVolume != 0.0) {
                double percentChange = getPercentChange(prevDayVolume, newDayVolume);
                CoinDataFor24Hr coinDataFor24Hr = data.stream().filter(d -> d.getSymbol().equals(coin)).findFirst().orElse(new CoinDataFor24Hr());
                NumberFormat nf = new DecimalFormat("##.##");
                percentChange = Double.parseDouble(nf.format(percentChange));
                coinDataFor24Hr.setVolumeChangePercent(percentChange);
            }
        });
    }

    @Cacheable(cacheNames = {"All24HourTicker", "VolumeCache"})
    public List<CoinDataFor24Hr> get24HrAllCoinTicker() {
        String url = symbol24HrAllTickerUrl;
        ResponseEntity<LinkedHashMap[]> info = restTemplate.getForEntity(url, LinkedHashMap[].class);
        LinkedHashMap[] body = info.getBody();
        if (body == null) {
            return new ArrayList<>();
        }

        List<CoinDataFor24Hr> list = new ArrayList<>();
        for (LinkedHashMap map : body) {
            CoinDataFor24Hr coin = get24HrCoinTicker(map);
            list.add(coin);
        }
        //todo: add volume change
        add24HrVolumeChange(list);
        return list;
    }

    public byte[] getIconBytes(String coin) {
        //Attempt to get the icon out of the cache if it is in there.
        //If not in the cache, then call the icon extract service and add the icon bytes to the cache.
        //The data in the cache will expire according to the setup in the CachingConfig configuration.
        Cache volumeCache = cacheManager.getCache("IconCache");
        byte[] coins = null;
        if (volumeCache != null) {
            Cache.ValueWrapper value = volumeCache.get(coin);
            if (value == null) {
                try {
                    coins = IconExtractor.getIconBytes(coin);
                } catch (IOException e) {
                    e.printStackTrace();
                    //here, the coin icon wasn't in the zip file -
                    // add a non-null empty array to the cache so we don't keep trying to extract it from the zip file
                    coins = new byte[0];
                }
                volumeCache.putIfAbsent(coin, coins);
            } else {
                coins = (byte[]) value.get();
            }
        }
        return coins;
    }
}
