package com.scanner.cryptoserver.util;

import com.scanner.cryptoserver.exchange.binance.dto.CoinTicker;

import java.util.List;

/**
 * Relative Strength Index. Implemented up to this specification:
 * http://en.wikipedia.org/wiki/Relative_strength
 */
public class Rsi {
    private final List<CoinTicker> tickers;

    public Rsi(final List<CoinTicker> tickers) {
        this.tickers = tickers;
    }

    public double calculate(int periodLength) {
        int tickerSize = tickers.size();
        int lastBar = tickerSize - 1;
        int firstBar = lastBar - periodLength + 1;
        double gains = 0, losses = 0;

        for (int bar = firstBar + 1; bar <= lastBar; bar++) {
            double change = tickers.get(bar).getClose() - tickers.get(bar - 1).getClose();
            gains += Math.max(0, change);
            losses += Math.max(0, -change);
        }

        double change = gains + losses;
        double value = (change == 0) ? 50 : (100 * gains / change);
        return value;
    }
}