package com.scanner.cryptoserver.exchange.binance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr;
import com.scanner.cryptoserver.exchange.binance.dto.CoinTicker;
import com.scanner.cryptoserver.exchange.binance.dto.ExchangeInfo;
import com.scanner.cryptoserver.util.SandboxUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The purpose of a Sandbox exchange is to return data for Binance USA but without
 * calling the Binance USA exchange. This is done to avoid using Binance USA API quotas.
 * For example, if the client is making lots of changes and the client doesn't need up-to-date data,
 * then it would be wise to use the Sandbox data so that API calls are prevented.
 * The data in the Sandbox is actual data from a past API call to Binance USA that is stored in files.
 * The Sandbox data never changes - it is static.
 */
@Service(value = "sandboxBinanceUsaService")
public class SandboxBinanceUsaExchangeService implements BinanceExchangeService {
    private static final Logger Log = LoggerFactory.getLogger(SandboxBinanceUsaExchangeService.class);
    private final SandboxUtil sandboxUtil;
    private ObjectMapper objectMapper;

    public SandboxBinanceUsaExchangeService(SandboxUtil sandboxUtil) {
        this.sandboxUtil = sandboxUtil;
        objectMapper = new ObjectMapper();
    }

    private <T> T getData(String name, Class<T> theClass) {
        String json = sandboxUtil.getJson(name);
        T data = null;

        try {
            data = objectMapper.readValue(json, theClass);
        } catch (
                JsonProcessingException e) {
            Log.error("Error trying to parse Binance USA Sandbox data: {} error: {}", name, e.getMessage());
        }
        return data;
    }

    private <T> List<T> getDataList(String name, Class<T> listClass) {
        String json = sandboxUtil.getJson(name);
        List<T> list = null;

        JavaType itemType = objectMapper.getTypeFactory().constructCollectionType(List.class, listClass);
        try {
            list = objectMapper.readValue(json, itemType);
        } catch (JsonProcessingException e) {
            Log.error("Error trying to parse Binance USA Sandbox data: {} error: {}", name, e.getMessage());
        }
        return list;
    }

    @Override
    public ExchangeInfo getExchangeInfo() {
        return getData("binanceusa-exchangeInfo", ExchangeInfo.class);
    }

    @Override
    public CoinDataFor24Hr call24HrCoinTicker(String symbol) {
        return getData("binanceusa-24HourTicker-" + symbol, CoinDataFor24Hr.class);
    }

    @Override
    public List<CoinDataFor24Hr> get24HrAllCoinTicker() {
        return getDataList("binanceusa-24HourTicker", CoinDataFor24Hr.class);
    }

    @Override
    public List<CoinTicker> getTickerData(String symbol, String interval, String daysOrMonths) {
        return getDataList("binanceusa-dayTicker-" + symbol + "-" + interval + "-" + daysOrMonths, CoinTicker.class);
    }
}
