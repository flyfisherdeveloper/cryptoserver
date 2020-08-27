package com.scanner.cryptoserver.exchange.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr;
import com.scanner.cryptoserver.exchange.binance.dto.CoinTicker;
import com.scanner.cryptoserver.exchange.binance.service.SandboxBinanceUsaExchangeService;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.ExchangeInfo;
import com.scanner.cryptoserver.util.RsiCalc;
import com.scanner.cryptoserver.util.SandboxUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public abstract class AbstractSandboxExchangeService implements ExchangeService {
    private static final Logger Log = LoggerFactory.getLogger(SandboxBinanceUsaExchangeService.class);
    private final SandboxUtil sandboxUtil;
    private final ObjectMapper objectMapper;

    public AbstractSandboxExchangeService(SandboxUtil sandboxUtil) {
        this.sandboxUtil = sandboxUtil;
        objectMapper = new ObjectMapper();
    }

    protected <T> T getData(String name, Class<T> theClass) {
        String json = sandboxUtil.getJson(name);
        T data = null;

        try {
            data = objectMapper.readValue(json, theClass);
        } catch (
                JsonProcessingException e) {
            Log.error("Error trying to parse Sandbox data: {} error: {}", name, e.getMessage());
        }
        return data;
    }

    protected <T> List<T> getDataList(String name, Class<T> listClass) {
        String json = sandboxUtil.getJson(name);
        List<T> list = null;

        JavaType itemType = objectMapper.getTypeFactory().constructCollectionType(List.class, listClass);
        try {
            list = objectMapper.readValue(json, itemType);
        } catch (JsonProcessingException e) {
            Log.error("Error trying to parse Sandbox list data: {} error: {}", name, e.getMessage());
        }
        return list == null ? new ArrayList<>() : list;
    }

    @Override
    public ExchangeInfo getExchangeInfo() {
        return getData(getDataName("exchangeInfo"), ExchangeInfo.class);
    }

    @Override
    public ExchangeInfo retrieveExchangeInfoFromCache() {
        return getExchangeInfo();
    }

    @Override
    public Supplier<ExchangeInfo> getExchangeInfoSupplier() {
        return null;
    }


    @Override
    public CoinDataFor24Hr get24HourCoinData(String symbol) {
        return getData(getDataName("24HourTicker-" + symbol), CoinDataFor24Hr.class);
    }

    @Override
    public List<CoinDataFor24Hr> get24HrAllCoinTicker() {
        return getDataList(getDataName("24HourTicker"), CoinDataFor24Hr.class);
    }

    @Override
    public List<CoinDataFor24Hr> get24HrAllCoinTicker(int page, int pageSize) {
        List<CoinDataFor24Hr> coins = get24HrAllCoinTicker();
        if (page < 0) {
            return coins;
        }
        int start = page * pageSize;
        int end = page * pageSize + pageSize;
        if (start > coins.size()) {
            return Collections.emptyList();
        }
        if (end > coins.size()) {
            end = coins.size();
        }
        return coins.subList(start, end);
    }

    @Override
    public List<CoinTicker> getTickerData(String symbol, String interval, String daysOrMonths) {
        return getDataList(getDataName("dayTicker-" + symbol + "-" + interval + "-" + daysOrMonths), CoinTicker.class);
    }

    @Override
    public void setRsiForTickers(List<CoinTicker> tickers, int periodLength) {
        RsiCalc rsi = new RsiCalc();
        rsi.calculateRsiForTickers(tickers, periodLength);
    }

    @Override
    public List<CoinTicker> getRsiTickerData(List<String> symbols) {
        return getDataList(getDataName("rsiTickers"), CoinTicker.class);
    }

    protected String getDataName(String name) {
        return getSandboxName() + "-" + name;
    }

    protected abstract String getSandboxName();
}
