package com.scanner.cryptoserver.exchange.binance.us.service;

import com.scanner.cryptoserver.exchange.AbstractExchangeService;
import com.scanner.cryptoserver.exchange.binance.us.dto.CoinDataFor24Hr;
import com.scanner.cryptoserver.exchange.binance.us.dto.CoinTicker;
import com.scanner.cryptoserver.exchange.binance.us.dto.ExchangeInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service(value="binanceUsaService")
public class BinanceUsaExchangeService extends AbstractExchangeService {
    @Value("${exchanges.binance.info}")
    private String exchangeInfoUrl;
    @Value("${exchanges.binance.klines}")
    private String klinesUrl;
    @Value("${exchanges.binance.ticker}")
    private String tickerUrl;
    @Value("${exchanges.binance.trade}")
    private String tradeUrl;
    @Value("${environments.icon}")
    private String iconUrl;

    public BinanceUsaExchangeService(CacheManager cacheManager, RestOperations restTemplate) {
        super(cacheManager, restTemplate);
    }

    public ExchangeInfo getExchangeInfo() {
        ResponseEntity<ExchangeInfo> info = restTemplate.getForEntity(exchangeInfoUrl, ExchangeInfo.class);
        return info.getBody();
    }

    public List<CoinTicker> getCoinTicker(String symbol, String interval) {
        return callCoinTicker(symbol, interval, null, null);
    }

    public CoinDataFor24Hr call24HrCoinTicker(String symbol) {
        String url = tickerUrl + "/24hr?symbol={symbol}";
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
        int offset = 3;
        if (symbol.endsWith("USDT") || (symbol.endsWith("BUSD") && !symbol.startsWith("BNB"))) {
            offset = 4;
        }
        return offset;
    }

    private int getStartOfQuote(String str) {
        int end = str.length();
        int offset = this.getQuoteOffset(str);
        return end - offset;
    }

    private String getQuote(String str) {
        int start = this.getStartOfQuote(str);
        return str.substring(start);
    }

    private String getCoin(String str) {
        int offset = this.getStartOfQuote(str);
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

        data.setupLinks(tradeUrl, iconUrl);
        byte[] iconBytes = getIconBytes(coin);
        data.setIcon(iconBytes);

        return data;
    }

    @Override
    protected List<CoinTicker> callCoinTicker(String symbol, String interval, Long startTime, Long endTime) {
        if (interval.equals("24h")) {
            interval = "1d";
        }
        String url = klinesUrl + "?symbol={symbol}&interval={interval}";
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("interval", interval);
        if (startTime != null) {
            params.put("startTime", startTime);
            String symbolTickerTimeParams = "&startTime={startTime}&endTime={endTime}";
            url += symbolTickerTimeParams;
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

    public void add24HrVolumeChange(List<CoinDataFor24Hr> data) {
        List<String> coins = data.stream().map(CoinDataFor24Hr::getSymbol).collect(Collectors.toList());

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

    @Override
    public List<CoinTicker> getTickerData(String symbol, String interval, String daysOrMonths) {
        return getTickerData("binanceCoinCache", symbol, interval, daysOrMonths);
    }

    //The cache keeps data as defined in the Cache config. When the data in the cache expires, the call
    //to extract new data will take place here. This will suffice for now, as the solution is new,
    //but if the solution and website ever grows, a new solution will be needed. We would need to create a running
    //thread that extracts data from the exchange api service frequently: for example, once a minute or so.
    //Since this solution now is just for "starters" and is just a "show and tell" type of solution, we
    //will avoid calling the exchange api frequently (i.e. once a minute) for now.
    @Cacheable(cacheNames = {"All24HourTicker", "CoinCache"})
    public List<CoinDataFor24Hr> get24HrAllCoinTicker() {
        String url = tickerUrl + "/24hr";
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
        add24HrVolumeChange(list);
        return list;
    }
}
