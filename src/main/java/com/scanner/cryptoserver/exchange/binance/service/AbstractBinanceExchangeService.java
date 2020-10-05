package com.scanner.cryptoserver.exchange.binance.service;

import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr;
import com.scanner.cryptoserver.exchange.binance.dto.CoinTicker;
import com.scanner.cryptoserver.exchange.coinmarketcap.CoinMarketCapService;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapListing;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.ExchangeInfo;
import com.scanner.cryptoserver.exchange.service.ExchangeService;
import com.scanner.cryptoserver.exchange.service.ExchangeVisitor;
import com.scanner.cryptoserver.util.CacheUtil;
import com.scanner.cryptoserver.util.RsiCalc;
import com.scanner.cryptoserver.util.dto.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestOperations;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * This service makes api calls for Binance exchanges: both BinanceUSA and Binance.
 * Each exchange has its separate URL, which are encapsulated in the BinanceService classes for the respective exchange.
 */
public abstract class AbstractBinanceExchangeService implements ExchangeService {
    private static final Logger Log = LoggerFactory.getLogger(AbstractBinanceExchangeService.class);
    private static final String ALL_24_HOUR_TICKER = "All24HourTicker";
    private static final String ALL_TICKERS = "AllTickers";
    private static final String EXCHANGE_INFO = "ExchangeInfo";
    private static final String COIN_CACHE = "CoinCache";
    private static final int ALL_24_HOUR_MAX_COUNT = 6;
    private static final int ALL_24_HOUR_DELAY = 151;
    private static final String TRADING = "TRADING";
    private static final List<String> nonUsaMarkets = Arrays.asList("NGN", "RUB", "TRY", "EUR", "ZAR", "BKRW", "IDRT", "UAH", "BIDR", "GBP", "AUD");

    private final RestOperations restTemplate;
    private final CoinMarketCapService coinMarketCapService;
    private final CacheUtil cacheUtil;

    public AbstractBinanceExchangeService(RestOperations restTemplate, CoinMarketCapService coinMarketCapService, CacheUtil cacheUtil) {
        this.restTemplate = restTemplate;
        this.coinMarketCapService = coinMarketCapService;
        this.cacheUtil = cacheUtil;
    }

    @Override
    public Supplier<ExchangeInfo> getExchangeInfoSupplier() {
        return () -> {
            ResponseEntity<ExchangeInfo> response = restTemplate.getForEntity(getUrlExtractor().getExchangeInfoUrl(), ExchangeInfo.class);
            ExchangeInfo info = response.getBody();
            return info;
        };
    }

    /**
     * Get exchange information. Gets the information out of the cache if in there.
     *
     * @return The exchange information.
     */
    @Override
    public ExchangeInfo retrieveExchangeInfoFromCache() {
        String name = getExchangeName() + "-" + EXCHANGE_INFO;
        ExchangeInfo exchangeInfo = cacheUtil.retrieveFromCache(EXCHANGE_INFO, name, getExchangeInfoSupplier());
        return exchangeInfo;
    }

    /**
     * When coins have duplicate symbols, such as "UNI", this visitor is used by services
     * to determine which coin is wanted for a given symbol that has multiple coins.
     * The coin wanted is the coin name, which is assumbed to be unique. Thus, the coin
     * symbol/name pair should suffice for determining exactly which coin is wanted.
     * Since the exchange does not have the coin name in its API data, (only the symbol is there),
     * we simply must hard-code the values here, until the exchanges give more information in their
     * API data.
     *
     * @return the name of the coin, such as "Uniswap", or "Universe" for a given coin
     * symbol, such as "UNI".
     */
    @Override
    //todo: jeff unit test this
    public ExchangeVisitor getExchangeVisitor() {
        return (coin) -> {
            if (coin == null) {
                return "";
            }
            if (coin.equals("UNI")) {
                return "Uniswap";
            }
            return coin;
        };
    }

    private void setMarketCapForExchangeInfo(ExchangeInfo exchangeInfo) {
        CoinMarketCapListing coinMarketCap = coinMarketCapService.getCoinMarketCapListing();

        if (coinMarketCap != null) {
            //If the coin market cap data exists, then update each symbol with the market cap value found in the market cap data.
            exchangeInfo.getSymbols().forEach(symbol -> symbol.addMarketCapAndId(getExchangeVisitor(), coinMarketCap));
        }
    }

    /**
     * Get exchange information. Gets the information out of the cache if in there.
     * Sets some information in the exchange info for the coins (symbols) in it.
     *
     * @return The exchange information.
     */
    public ExchangeInfo getExchangeInfo() {
        ExchangeInfo exchangeInfo = getExchangeInfoWithoutMarketCap();
        setMarketCapForExchangeInfo(exchangeInfo);
        return exchangeInfo;
    }

    /**
     * Get exchange information. Gets the information out of the cache if in there.
     * Does NOT make a call to supply the market cap info - when the exchange info is in the cache, the market cap is already set.
     *
     * @return The exchange information.
     */
    public ExchangeInfo getExchangeInfoWithoutMarketCap() {
        ExchangeInfo exchangeInfo = retrieveExchangeInfoFromCache();
        //remove currency markets that are not USA-based, such as the Euro ("EUR")
        exchangeInfo.getSymbols().removeIf(s -> nonUsaMarkets.contains(s.getQuoteAsset()));
        return exchangeInfo;
    }

    public Set<String> getMarkets() {
        ExchangeInfo exchangeInfo = retrieveExchangeInfoFromCache();
        Set<String> set = exchangeInfo.getSymbols().stream().map(Symbol::getQuoteAsset).collect(Collectors.toSet());
        return set;
    }

    public List<CoinTicker> getCoinTicker(String symbol, String interval) {
        return callCoinTicker(symbol, interval, null, null);
    }

    public CoinDataFor24Hr get24HourCoinData(String symbol) {
        String url = getUrlExtractor().getTickerUrl() + "/24hr?symbol={symbol}";
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        ResponseEntity<LinkedHashMap> info = restTemplate.getForEntity(url, LinkedHashMap.class, params);
        LinkedHashMap body = info.getBody();
        if (body == null) {
            return new CoinDataFor24Hr();
        }
        CoinDataFor24Hr coin = get24HrCoinTicker(body);
        coinMarketCapService.setMarketCapDataFor24HrData(getExchangeVisitor(), coin);
        return coin;
    }

    /**
     * The symbol will be the coin/quote(market), such as "BTCUSD".
     * We need to split this into coin and quote(market). So, this method determines the offset
     * to split the string, such as for "BTCUSD", the offset would be 3; for "BTCUSDT",
     * the offset would be 4.
     *
     * @param symbol the symbold, such as "BTCUSDT".
     * @return the offset where the coin/quote are split.
     */
    private int getQuoteOffset(String symbol) {
        //get the set of markets for the exchange
        Set<String> markets = getMarkets();

        //note: this is a special case
        //There are coins whose market is "BUSD". But how do we determine the difference
        //between a market of "BUSD" or "USD"? We really can't, except to assume that
        //the symbol is longer than 6 characters (such as "ZILBUSD") to differentiate
        //between "BNBUSD" which is only 6 charachters long, and the market for that is "USD" and not "BUSD".
        //This really is a hack, without much of an alternative, until the exchange gives better information for symbols,
        //such as "ZIL-BUSD" instead of "ZILBUSD", or "BNB-USD" instead of "BNBUSD".
        if (symbol.endsWith("BUSD") && symbol.length() > 6) {
            return 4;
        }
        //stream through the markets and find which one matches this symbol
        int offset = markets.stream()
                .filter(symbol::endsWith)
                .findFirst()
                .map(String::length)
                .orElse(3);
        return offset;
    }

    private int getStartOfQuote(String str) {
        int end = str.length();
        int offset = this.getQuoteOffset(str);
        return end - offset;
    }

    public String getQuote(String str) {
        ExchangeInfo exchangeInfo = retrieveExchangeInfoFromCache();
        Supplier<String> parseQuote = () -> {
            int start = this.getStartOfQuote(str);
            return str.substring(start);
        };
        String quote = exchangeInfo.getSymbols().stream()
                .filter(sym -> sym.getSymbol() != null && sym.getSymbol().equals(str))
                .findFirst()
                .map(Symbol::getQuoteAsset)
                .orElseGet(parseQuote);
        return quote;
    }

    public String getCoin(String str) {
        ExchangeInfo exchangeInfo = retrieveExchangeInfoFromCache();
        Supplier<String> parseCoin = () -> {
            int offset = this.getStartOfQuote(str);
            return str.substring(0, offset);
        };
        String coin = exchangeInfo.getSymbols().stream()
                .filter(sym -> sym.getSymbol() != null && sym.getSymbol().equals(str))
                .findFirst()
                .map(Symbol::getBaseAsset)
                .orElseGet(parseCoin);
        return coin;
    }

    /**
     * Exclude coins that are not currently trading, but exist on the exchange.
     *
     * @param symbol the coin, such as BTCUSDT.
     * @return true if the coin is actively trading, false otherwise.
     */
    private boolean isCoinTrading(String symbol) {
        ExchangeInfo info = retrieveExchangeInfoFromCache();
        boolean trading = info.getSymbols().stream()
                .anyMatch(s -> s.getSymbol().equals(symbol) && s.getStatus().equals(TRADING));
        return trading;
    }

    /**
     * Returns true if the symbol is a leveraged token, such as ETHBULL or ETHDOWN.
     * A leveraged token is a trading pair based on a regular coin (such as ETH), but is subject
     * to leveraged trading, and is not an ordinary coin. Therefore, we don't return these to the
     * client. Unfortunately, it appears there isn't anything definitive from the exchange api
     * to determine these tokens, other than checking their name for "BULL" or "BEAR", which is
     * somewhat of a "hack", but is the best available method now due to api limitations.
     * This will break of course if ever a coin is introduced that includes the name "BULL" or "BEAR".
     * No such coin exists on the exchanges as of now, however.
     *
     * @param symbol the trading symbol, such as ETHBTC or BTCUSDT
     * @return true if the symbol represents a leveraged token; false otherwise.
     */
    private boolean isLeveragedToken(String symbol) {
        return symbol.endsWith("DOWN") || symbol.endsWith("UP") || symbol.contains("BULL") || symbol.contains("BEAR");
    }

    private boolean isCoinInUsaMarket(String currency) {
        return !nonUsaMarkets.contains(currency);
    }

    private CoinDataFor24Hr get24HrCoinTicker(LinkedHashMap map) {
        CoinDataFor24Hr data = new CoinDataFor24Hr();
        String symbol = (String) map.get("symbol");
        String coin = getCoin(symbol);
        String currency = getQuote(symbol);
        if (!isCoinTrading(symbol) || !isCoinInUsaMarket(currency) || isLeveragedToken(symbol) || isLeveragedToken(coin)) {
            return null;
        }

        data.setSymbol(symbol);
        data.setCoin(coin);
        data.setCurrency(currency);

        String priceChangeStr = (String) map.get("priceChange");
        double priceChange = Double.parseDouble(priceChangeStr);
        data.setPriceChange(priceChange);

        String priceChangePercentStr = (String) map.get("priceChangePercent");
        double priceChangePercent = Double.parseDouble(priceChangePercentStr);
        data.setPriceChangePercent(priceChangePercent);
        NumberFormat nf = new DecimalFormat("##.##");
        priceChangePercent = Double.parseDouble(nf.format(priceChangePercent));
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

        data.setupLinks(getUrlExtractor().getTradeUrl());
        byte[] iconBytes = cacheUtil.getIconBytes(coin);
        data.setIcon(iconBytes);

        return data;
    }

    private List<CoinTicker> callCoinTicker(String symbol, String interval,
                                            long startTime1, long toTime1, long startTime2, long toTime2) {
        List<CoinTicker> coinTickers = new ArrayList<>();

        if (startTime2 != 0 && toTime2 != 0) {
            CompletableFuture<List<CoinTicker>> call1 = CompletableFuture.supplyAsync(() -> callCoinTicker(symbol, interval, startTime1, toTime1));
            CompletableFuture<List<CoinTicker>> call2 = CompletableFuture.supplyAsync(() -> callCoinTicker(symbol, interval, startTime2, toTime2));
            CompletableFuture<Void> allCalls = CompletableFuture.allOf(call1, call2);
            allCalls.thenRun(() -> {
                try {
                    coinTickers.addAll(call1.get());
                    coinTickers.addAll(call2.get());
                    //sort the list - since they are run asynchronously, to ensure the final list is in order
                    coinTickers.sort(Comparator.comparingLong(CoinTicker::getCloseTime));
                } catch (InterruptedException | ExecutionException e) {
                    Log.error("Error: {} " + e.getMessage());
                }
            }).join();
        } else {
            coinTickers.addAll(callCoinTicker(symbol, interval, startTime1, toTime1));
        }
        return coinTickers;
    }

    public List<CoinTicker> callCoinTicker(String symbol, String interval, Long startTime, Long endTime) {
        if (interval.equals("24h")) {
            interval = "1d";
        }
        String url = getUrlExtractor().getKlinesUrl() + "?symbol={symbol}&interval={interval}";
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

    /**
     * Call the coin ticker for the symbol using the interval over the days/months specified.
     *
     * @param symbol       The coin, such as "LTCUSDT".
     * @param interval     The interval string such as "12h" (12 hours).
     * @param daysOrMonths The days or months string, such as "30d" (thirty days) or "3m" (three months).
     * @return The coin tickers for the interval and period specified.
     */
    public List<CoinTicker> callCoinTicker(String symbol, String interval, String daysOrMonths) {
        Instant now = Instant.now();
        Instant from;
        long startTime1;
        long toTime1 = now.toEpochMilli();
        long startTime2 = 0;
        long toTime2 = 0;

        if (daysOrMonths.endsWith("d")) {
            int numDays = Integer.parseInt("" + daysOrMonths.charAt(0));
            from = now.minus(numDays, ChronoUnit.DAYS);
            startTime1 = from.toEpochMilli();
        } else {
            return callCoinTickerForMonths(symbol, interval, daysOrMonths);
        }
        List<CoinTicker> coinTickers = callCoinTicker(symbol, interval, startTime1, toTime1, startTime2, toTime2);
        return coinTickers;
    }

    /**
     * Get the number of days from today going back a number of months.
     *
     * @param months the number of months to go back.
     * @return the number of days going back.
     */
    public int getDaysBetween(int months) {
        LocalDate now = LocalDate.now();
        LocalDate start = LocalDate.now().minusMonths(months);
        return Math.toIntExact(ChronoUnit.DAYS.between(start, now));
    }

    /**
     * Call the monthly coin ticker for the symbol using the interval and months.
     *
     * @param symbol   The coin, such as "BTCUSD".
     * @param interval The interval string, such as "4h" (4 hours).
     * @param months   The months string, such as "3M".
     * @return The coin tickers for the month period using the interval.
     */
    private List<CoinTicker> callCoinTickerForMonths(String symbol, String interval, String months) {
        final int hoursInDay = 24;
        final int maxDataPoints = 500;
        Instant now = Instant.now();
        Instant from;
        long toTime1 = now.toEpochMilli();
        long startTime1;
        long startTime2 = 0;
        long toTime2 = 0;
        //we use lower-case letters for hours, and upper-case letters for months (to distinguish from Minutes ("m") if we ever use it)
        interval = interval.replace("H", "h");
        months = months.replace("m", "M");
        int hours = Integer.parseInt(interval.substring(0, interval.indexOf("h")));
        int numMonths = Integer.parseInt("" + months.substring(0, months.indexOf("M")));
        int numberOfDays = getDaysBetween(numMonths);
        int numDataPoints = numberOfDays * hoursInDay / hours;

        //we want at most 2 calls in parallel - this is too prevent calling the binance server too much and getting rejected
        if (numDataPoints > maxDataPoints * 2) {
            String message = String.format("Too much data requested for %s with interval %s and months %s", symbol, interval, months);
            throw new RuntimeException(message);
        }

        from = now.minus(numberOfDays, ChronoUnit.DAYS);
        //If the number of data points required > 500, then we need two calls.
        //This is because the Binance.us api only brings back 500 data points, but more than that are needed.
        //Therefore, two calls are needed - go back 15 days for one call, then 15 to 30 days for the other call.
        //Then, combine the results from the two calls, sorting on the close time.
        if (numDataPoints >= maxDataPoints) {
            int midpoint = numberOfDays / 2;
            Instant fromMidpoint = now.minus(midpoint, ChronoUnit.DAYS);
            startTime1 = fromMidpoint.toEpochMilli();
            startTime2 = from.toEpochMilli();
            toTime2 = startTime1;
        } else {
            startTime1 = from.toEpochMilli();
        }
        List<CoinTicker> coinTickers = callCoinTicker(symbol, interval, startTime1, toTime1, startTime2, toTime2);
        return coinTickers;
    }

    public List<CoinTicker> getTickerData(String symbol, String interval, String daysOrMonths) {
        //Attempt to get the data out of the cache if it is in there.
        //If not in the cache, then call the service and add the data to the cache.
        //The data in the cache will expire according to the setup in the CachingConfig configuration.
        String name = getExchangeName() + "-" + symbol + interval + daysOrMonths;
        Supplier<List<CoinTicker>> coinTickerSupplier = () -> callCoinTicker(symbol, interval, daysOrMonths);
        List<CoinTicker> coins = cacheUtil.retrieveFromCache(COIN_CACHE, name, coinTickerSupplier);
        if (coins == null) {
            return new ArrayList<>();
        }
        return coins;
    }

    @Override
    public void setRsiForTickers(List<CoinTicker> tickers, int periodLength) {
        RsiCalc rsiCalc = new RsiCalc();
        rsiCalc.calculateRsiForTickers(tickers, periodLength);
    }

    @Override
    public List<CoinTicker> getRsiTickerData(List<String> symbols) {
        List<CoinTicker> list = new ArrayList<>();
        //return if too many symbols will go over the exchange quota
        //todo: add error message or exception
        if (symbols.size() > 15) {
            return list;
        }
        symbols.parallelStream().forEach(symbol -> {
            List<CoinTicker> tickers = getTickerData(symbol, "4h", "3M");
            list.addAll(tickers);
            tickers = getTickerData(symbol, "12h", "6M");
            list.addAll(tickers);
            tickers = getTickerData(symbol, "24h", "12M");
            list.addAll(tickers);
        });
        return list;
    }

    //The cache keeps data as defined in the Cache config. When the data in the cache expires, the call
    //to extract new data will take place here. This will suffice for now, as the solution is new,
    //but if the solution and website ever grows, a new solution will be needed. We would need to create a running
    //thread that extracts data from the exchange api service frequently: for example, once a minute or so.
    //For now, we do one-minute extractions from the api over a 15-minute interval.
    //Since this solution now is just for "starters" and is just a "show and tell" type of solution, we
    //will avoid calling the exchange api frequently (i.e. once a minute always) for now.
    public List<CoinDataFor24Hr> get24HrAllCoinTicker() {
        String cacheName = getExchangeName() + "-" + ALL_24_HOUR_TICKER;
        Supplier<List<CoinDataFor24Hr>> allCoinTicker = this::get24HrCoinData;
        List<CoinDataFor24Hr> coinDataFor24Hrs = cacheUtil.retrieveFromCache(cacheName, ALL_TICKERS, allCoinTicker);
        return coinDataFor24Hrs;
    }

    public List<CoinDataFor24Hr> get24HrAllCoinTicker(int page, int pageSize) {
        String cacheName = getExchangeName() + "-" + ALL_24_HOUR_TICKER;
        Supplier<LinkedHashMap[]> allCoinTicker = this::get24HrData;
        LinkedHashMap[] data = cacheUtil.retrieveFromCache(cacheName, ALL_TICKERS, allCoinTicker);
        return get24HrCoinData(data, page, pageSize);
    }

    public LinkedHashMap[] get24HrData() {
        String url = getUrlExtractor().getTickerUrl() + "/24hr";
        ResponseEntity<LinkedHashMap[]> info = restTemplate.getForEntity(url, LinkedHashMap[].class);
        LinkedHashMap[] body = info.getBody();
        return body;
    }

    public List<CoinDataFor24Hr> get24HrCoinData(LinkedHashMap[] data, int page, int pageSize) {
        if (data == null) {
            data = get24HrData();
            if (data == null) {
                return null;
            }
        }

        //go through the data array, and return those in the page
        List<CoinDataFor24Hr> list = new ArrayList<>();
        int lastIndex = page * pageSize;
        if (page == -1) {
            lastIndex = data.length;
            pageSize = data.length;
        }
        for (int index = lastIndex - pageSize; index < (Math.min(lastIndex, data.length)); index++) {
            CoinDataFor24Hr coin = get24HrCoinTicker(data[index]);
            if (coin != null) {
                list.add(coin);
            }
        }

        coinMarketCapService.setMarketCapDataFor24HrData(getExchangeVisitor(), list);
        //since this is the first time (in awhile) we have called the exchange info,
        //start threads to update every minute for 15 minutes - this way the client gets
        //updated 24-hour data every minute
        //We stop at 15 minutes just to prevent too much data from being processed (we are trying to stay in the AWS free zone!),
        // and we are trying to not go over quota on extracting exchange data.

        startUpdates();
        return list;
    }

    public List<CoinDataFor24Hr> get24HrCoinData() {
        return get24HrCoinData(null, -1, -1);
    }

    //Run the scheduled service call and put the results in the cache. Stop the scheduler after a maximum times of running.
    private void runScheduled24HrAllCoinTicker() {
        incrementAll24HourTickerCounter();
        String cacheName = getExchangeName() + "-" + ALL_24_HOUR_TICKER;
        //the number of times it is run * the delay interval is about 15 minutes
        //so, after 15 minutes, the process will be shutdown
        if (getAll24HourTickerCount() >= ALL_24_HOUR_MAX_COUNT) {
            Log.debug("Shutting down scheduler executor for {} exchange", getExchangeName());
            cacheUtil.evict(cacheName, ALL_TICKERS);
            shutdownScheduledService();
        } else {
            CacheUtil.CacheCommand command = this::get24HrAllCoinTicker;
            cacheUtil.evictAndThen(cacheName, ALL_TICKERS, command);
        }
    }

    //Run a scheduler to update the 24-hour exchange coin ticker.
    //The purpose of this is to extract data every 2.5 minutes, and save it in the cache.
    //This goes for 15 minutes - we would do longer, but the API's severely limit how much data can be extracted.
    private void startUpdates() {
        ScheduledExecutorService scheduledService = getScheduledService();
        if (scheduledService != null) {
            //another scheduler is already executing - don't start another
            return;
        }
        Log.debug("Starting scheduler executor for {} exchange", getExchangeName());
        //todo: should the separate exchanges determine this? If so, it would encapsulate better.
        scheduledService = Executors.newScheduledThreadPool(1);
        Runnable command = this::runScheduled24HrAllCoinTicker;
        //run every 2.5 minutes
        scheduledService.scheduleAtFixedRate(command, ALL_24_HOUR_DELAY, ALL_24_HOUR_DELAY, TimeUnit.SECONDS);
        setScheduledService(scheduledService);
    }

    protected abstract UrlExtractor getUrlExtractor();

    public abstract String getExchangeName();

    protected abstract int getAll24HourTickerCount();

    protected abstract void incrementAll24HourTickerCounter();

    protected abstract void shutdownScheduledService();

    protected abstract ScheduledExecutorService getScheduledService();

    protected abstract void setScheduledService(ScheduledExecutorService scheduledService);
}
