package com.scanner.cryptoserver.exchange.binance.controller;

import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr;
import com.scanner.cryptoserver.exchange.binance.dto.CoinTicker;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.ExchangeInfo;
import com.scanner.cryptoserver.util.dto.Symbol;
import com.scanner.cryptoserver.exchange.service.ExchangeService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The purpose of a Sandbox exchange is to return data for Binance USA but without
 * calling the Binance USA exchange. This is done to avoid using Binance USA API quotas.
 * For example, if the client is making lots of changes and the client doesn't need up-to-date data,
 * then it would be wise to use the Sandbox data so that API calls are prevented.
 * The data in the Sandbox is actual data from a past API call to Binance USA that is stored in files.
 * The Sandbox data never changes - it is static.
 */
@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("sandbox-api/v1/binanceusa")
public class SandboxBinanceUsaExchangeController {
    private final ExchangeService sandboxBinanceUsaService;

    public SandboxBinanceUsaExchangeController(ExchangeService sandboxBinanceUsaService) {
        this.sandboxBinanceUsaService = sandboxBinanceUsaService;
    }

    @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Symbol> getExchangeInfo() {
        ExchangeInfo info = sandboxBinanceUsaService.getExchangeInfo();
        return info.getSymbols();
    }

    @GetMapping(value = "/24HourTicker/{symbol}", produces = MediaType.APPLICATION_JSON_VALUE)
    public CoinDataFor24Hr get24HourTicker(@PathVariable String symbol) {
        CoinDataFor24Hr data = sandboxBinanceUsaService.get24HourCoinData(symbol);
        return data;
    }

    /**
     * Gets all the coins and 24-hour data on the exchange.
     * NOTE: This call has the heaviest "weight" of all exchange calls: Use sparingly!
     *
     * @return a list of all coins on the exchange over the past 24 hours.
     */
    @GetMapping(value = "/24HourTicker", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CoinDataFor24Hr> getAll24HourTicker() {
        List<CoinDataFor24Hr> data = sandboxBinanceUsaService.get24HrAllCoinTicker();
        return data;
    }

    /**
     * Gets the coins for 24-hour data on the exchange in a page.
     *
     * @return a list of coins for a page on the exchange over the past 24 hours.
     */
    @GetMapping(value = "/24HourTicker/{page}/{pageSize}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CoinDataFor24Hr> getAll24HourTicker(@PathVariable int page, @PathVariable int pageSize) {
        List<CoinDataFor24Hr> data = sandboxBinanceUsaService.get24HrAllCoinTicker(page, pageSize);
        return data;
    }

    @GetMapping(value = "/DayTicker/{symbol}/{interval}/{daysOrMonths}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CoinTicker> getDayTicker(@PathVariable String symbol, @PathVariable String interval, @PathVariable String daysOrMonths) {
        List<CoinTicker> data = sandboxBinanceUsaService.getTickerData(symbol, interval, daysOrMonths);
        return data;
    }
}
