package com.scanner.cryptoserver.exchange.coinbasepro.controller;

import com.scanner.cryptoserver.exchange.ExchangeService;
import com.scanner.cryptoserver.exchange.binance.us.dto.CoinDataFor24Hr;
import com.scanner.cryptoserver.exchange.binance.us.dto.CoinTicker;
import com.scanner.cryptoserver.exchange.binance.us.dto.ExchangeInfo;
import com.scanner.cryptoserver.exchange.binance.us.dto.Symbol;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "https://develop.d3u11owfti6r3u.amplifyapp.com")
@RequestMapping("api/v1/coinbasepro")
public class CoinbaseProExchangeController {
    private final ExchangeService service;

    public CoinbaseProExchangeController(@Qualifier("CoinbasePro") ExchangeService service) {
        this.service = service;
    }

    @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Symbol> getExchangeInfo() {
        ExchangeInfo info = service.getExchangeInfo();
        return info.getSymbols();
    }

    @GetMapping(value = "/24HourTicker/{symbol}", produces = MediaType.APPLICATION_JSON_VALUE)
    public CoinDataFor24Hr get24HourTicker(@PathVariable String symbol) {
        return service.call24HrCoinTicker(symbol);
    }

    /**
     * Gets all the coins and 24-hour data on the exchange.
     * NOTE: This call has the heaviest "weight" of all exchange calls: Use sparingly!
     *
     * @return a list of all coins on the exchange over the past 24 hours.
     */
    @GetMapping(value = "/24HourTicker", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CoinDataFor24Hr> getAll24HourTicker() {
        return service.get24HrAllCoinTicker();
    }

    @GetMapping(value = "/DayTicker/{symbol}/{interval}/{daysOrMonths}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CoinTicker> getDayTicker(@PathVariable String symbol, @PathVariable String interval, @PathVariable String daysOrMonths) {
        return service.getTickerData(symbol, interval, daysOrMonths);
    }

    @GetMapping(value = "/icon/{coin}", produces = MediaType.IMAGE_PNG_VALUE)
    public @ResponseBody
    byte[] getIcon(@PathVariable String coin) {
        return service.getIconBytes(coin);
    }
}
