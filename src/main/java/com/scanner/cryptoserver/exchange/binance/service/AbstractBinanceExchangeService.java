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
import com.scanner.cryptoserver.util.dto.Coin;
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
    private static final List<String> nonUsaMarkets = Arrays.asList("NGN", "RUB", "TRY", "EUR", "ZAR", "BKRW", "IDRT", "UAH", "BIDR", "GBP", "AUD");

    private final RestOperations restTemplate;
    private final CoinMarketCapService coinMarketCapService;
    private final CacheUtil cacheUtil;
    private final ExchangeVisitor binanceExchangeVisitor;

    public AbstractBinanceExchangeService(RestOperations restTemplate, CoinMarketCapService coinMarketCapService, CacheUtil cacheUtil, ExchangeVisitor binanceExchangeVisitor) {
        this.restTemplate = restTemplate;
        this.coinMarketCapService = coinMarketCapService;
        this.cacheUtil = cacheUtil;
        this.binanceExchangeVisitor = binanceExchangeVisitor;
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

    @Override
    public ExchangeVisitor getExchangeVisitor() {
        return binanceExchangeVisitor;
    }

    private void setMarketCapForExchangeInfo(ExchangeInfo exchangeInfo) {
        CoinMarketCapListing coinMarketCap = coinMarketCapService.getCoinMarketCapListing(getExchangeVisitor());

        if (coinMarketCap != null) {
            //If the coin market cap data exists, then update each symbol with the market cap value found in the market cap data.
            exchangeInfo.getCoins().forEach(symbol -> symbol.addMarketCapAndId(getExchangeVisitor(), coinMarketCap));
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
        exchangeInfo.getCoins().removeIf(s -> nonUsaMarkets.contains(s.getQuoteAsset()));
        return exchangeInfo;
    }

    public Set<String> getMarkets() {
        ExchangeInfo exchangeInfo = retrieveExchangeInfoFromCache();
        Set<String> set = exchangeInfo.getCoins().stream().map(Coin::getQuoteAsset).collect(Collectors.toSet());
        return set;
    }

    public List<CoinTicker> getCoinTicker(String symbol, String interval) {
        return callCoinTicker(symbol, interval, null, null);
    }

    public Optional<CoinDataFor24Hr> get24HourCoinData(String symbol) {
        String url = getUrlExtractor().getTickerUrl() + "/24hr?symbol={symbol}";
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        ResponseEntity<LinkedHashMap> info = restTemplate.getForEntity(url, LinkedHashMap.class, params);
        LinkedHashMap body = info.getBody();
        if (body == null) {
            return Optional.empty();
        }
        final Optional<CoinDataFor24Hr> ticker = get24HrCoinTicker(body);
        ticker.ifPresent(c -> coinMarketCapService.setMarketCapDataFor24HrData(getExchangeVisitor(), c));
        return ticker;
    }

    /**
     * Get a coin from a symbol string. i.e. "BTCUSD" returns the coin for the BTC/USD pair.
     *
     * @param str the symbol, such as "ETHUSDT".
     * @return the coin from the symbol.
     */
    public Optional<Coin> getCoin(String str) {
        ExchangeInfo exchangeInfo = retrieveExchangeInfoFromCache();
        final Optional<Coin> coin = exchangeInfo.getCoins().stream()
                .filter(sym -> sym.getSymbol() != null && sym.getSymbol().equals(str))
                .findFirst();
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
        boolean trading = info.getCoins().stream()
                .anyMatch(coin -> coin.getSymbol().equals(symbol) && coin.isTrading());
        return trading;
    }

    private boolean isCoinInUsaMarket(String currency) {
        return !nonUsaMarkets.contains(currency);
    }

    private Optional<CoinDataFor24Hr> get24HrCoinTicker(LinkedHashMap map) {
        CoinDataFor24Hr data = new CoinDataFor24Hr();
        String symbol = (String) map.get("symbol");
        final Optional<Coin> coin = getCoin(symbol);
        String baseAsset = coin.map(Coin::getBaseAsset).orElse("");
        String quoteAsset = coin.map(Coin::getQuoteAsset).orElse("");
        if (!isCoinTrading(symbol) || !isCoinInUsaMarket(quoteAsset)) {
            return Optional.empty();
        }

        data.setSymbol(symbol);
        data.setCoin(baseAsset);
        data.setCurrency(quoteAsset);

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
        byte[] iconBytes = cacheUtil.getIconBytes(getExchangeVisitor().getSymbol(baseAsset), null);
        data.setIcon(iconBytes);

        return Optional.of(data);
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
        if (interval.equals("72h")) {
            interval = "3d";
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

    /**
     * Add the $USD volume to the coins.
     * Note: In some cases, the USD volume value is not available,
     * and the USDT (an extremely close approximation to USD) was used
     * to compute the USD value.
     *
     * @param coins      the coins that are being retrieved.
     * @param usdTickers the coins that contain the USD volume value.
     */
    void addUsdVolume(List<CoinTicker> coins, List<CoinTicker> usdTickers) {
        for (int index = 0; index < coins.size(); index++) {
            if (index < usdTickers.size()) {
                CoinTicker usdTicker = usdTickers.get(index);
                CoinTicker coinTicker = coins.get(index);
                double usdOpen = usdTicker.getOpen();
                double usdClose = usdTicker.getClose();
                double coinOpen = coinTicker.getOpen();
                double coinClose = coinTicker.getClose();
                //the USD volume is computed as the coin price * the usd price at open + the coin price * the usd price at close divided by 2
                double avg = (usdOpen * coinOpen + usdClose * coinClose) / 2.0;
                double vol = coinTicker.getVolume() * avg;
                coins.get(index).setUsdVolume(vol);
            }
        }
    }

    @Override
    public List<CoinTicker> getTickerData(String symbol, String interval, String daysOrMonths) {
        final Optional<Coin> coin = getCoin(symbol);
        return coin.map(theCoin -> getTickerData(theCoin, interval, daysOrMonths))
                .orElse(Collections.emptyList());
    }

    private List<CoinTicker> getTickerData(Coin coin, String interval, String daysOrMonths) {
        //Attempt to get the data out of the cache if it is in there.
        //If not in the cache, then call the service and add the data to the cache.
        //The data in the cache will expire according to the setup in the CachingConfig configuration.
        String name = getExchangeName() + "-" + coin.getSymbol() + interval + daysOrMonths;
        Supplier<List<CoinTicker>> coinTickerSupplier = () -> callCoinTicker(coin.getSymbol(), interval, daysOrMonths);
        List<CoinTicker> coins = cacheUtil.retrieveFromCache(COIN_CACHE, name, coinTickerSupplier);
        if (coins == null || coins.isEmpty()) {
            return new ArrayList<>();
        }
        //Here, we want the USD volume.
        String quote = coin.getQuoteAsset();
        if (quote.equals("USD") || quote.equals("USDT")) {
            return coins;
        }
        //Find the USD volume, and add it to the list.
        final ExchangeInfo exchangeInfo = getExchangeInfo();
        final String usdSymbol = quote + getUsdQuote();
        //We need the USD or USDT pair of the quote coin in order to get the USD price in order to compute the USD volume for the tickers.
        //For example, if the coin is ETHBTC, then we need to get the USD ticker values for BTCUSD (or BTCUSDT) in order to get the
        //USD prices over the interval to compute the USD volume for ETHBTC over the interval.
        //If that pair doesn't exist for some reason, then just ignore the USD volume.
        exchangeInfo.getCoins().stream().filter(exchangeCoin -> exchangeCoin.getSymbol().equals(usdSymbol)).findAny().ifPresent(usdPair -> {
            List<CoinTicker> dollarTickers = getTickerData(usdSymbol, interval, daysOrMonths);
            addUsdVolume(coins, dollarTickers);
        });
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
            get24HrCoinTicker(data[index]).ifPresent(list::add);
        }

        coinMarketCapService.setMarketCapDataFor24HrData(getExchangeVisitor(), list);
        //Some coins have icons by their id and not their name.
        //Here, those will have a null icon.
        //Attempt to find the icon with the id here.
        list.stream()
                .filter(coin -> coin.getIcon() == null || coin.getIcon().length == 0 && coin.getId() != null)
                .forEach(coin -> coin.setIcon(cacheUtil.getIconBytes(null, coin.getId())));

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

    /**
     * Retrieve a list of coins with icons, which is contained in each element in the list.
     *
     * @return a list of coins which contain the coin symbol, and icon bytes. Note: the other
     * information for the coin may or may not be present.
     */
    @Override
    public List<CoinDataFor24Hr> getIcons() {
        List<CoinDataFor24Hr> coinList = get24HrAllCoinTicker();
        //go through each coin and get the icon, if it is there
        coinList.forEach(coin -> {
            byte[] icon = cacheUtil.getIconBytes(coin.getSymbol(), coin.getId());
            coin.setIcon(icon);
        });
        return coinList;
    }

    /**
     * Retrieve a list of coins with no icons, which is contained in each element in the list as an empty byte array.
     *
     * @return a list of coins which contain the coin symbol, and the empty icon bytes. Note: the other
     * information for the coin may or may not be present.
     */
    @Override
    public List<CoinDataFor24Hr> getMissingIcons() {
        final List<CoinDataFor24Hr> icons = getIcons();
        final List<CoinDataFor24Hr> list = icons.stream()
                .filter(coin -> coin.getIcon() == null || coin.getIcon().length == 0)
                .collect(Collectors.toList());
        return list;
    }

    protected abstract UrlExtractor getUrlExtractor();

    public abstract String getExchangeName();

    protected abstract int getAll24HourTickerCount();

    protected abstract void incrementAll24HourTickerCounter();

    protected abstract void shutdownScheduledService();

    protected abstract ScheduledExecutorService getScheduledService();

    protected abstract void setScheduledService(ScheduledExecutorService scheduledService);

    protected abstract String getUsdQuote();
}
