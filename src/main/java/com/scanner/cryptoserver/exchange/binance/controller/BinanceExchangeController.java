package com.scanner.cryptoserver.exchange.binance.controller;

import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr;
import com.scanner.cryptoserver.exchange.binance.dto.CoinTicker;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.ExchangeInfo;
import com.scanner.cryptoserver.util.dto.Symbol;
import com.scanner.cryptoserver.exchange.service.ExchangeService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
//@CrossOrigin(origins = "https://develop.d2vswqrfiywrsc.amplifyapp.com")
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("api/v1/binance")
public class BinanceExchangeController {
    private final ExchangeService binanceService;

    public BinanceExchangeController(ExchangeService binanceService) {
        this.binanceService = binanceService;
    }

    @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Symbol> getExchangeInfo() {
        ExchangeInfo info = binanceService.getExchangeInfo();
        return info.getSymbols();
    }

    @GetMapping(value = "/24HourTicker/{symbol}", produces = MediaType.APPLICATION_JSON_VALUE)
    public CoinDataFor24Hr get24HourTicker(@PathVariable String symbol) {
        CoinDataFor24Hr data = binanceService.get24HourCoinData(symbol);
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
        List<CoinDataFor24Hr> data = binanceService.get24HrAllCoinTicker();
        return data;
    }

    @GetMapping(value = "/DayTicker/{symbol}/{interval}/{daysOrMonths}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CoinTicker> getDayTicker(@PathVariable String symbol, @PathVariable String interval, @PathVariable String daysOrMonths) {
        List<CoinTicker> data = binanceService.getTickerData(symbol, interval, daysOrMonths);
        return data;
    }
}
