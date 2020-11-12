package com.scanner.cryptoserver.exchange.binance.service;

import com.scanner.cryptoserver.exchange.service.ExchangeVisitor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class BinanceExchangeVisitor implements ExchangeVisitor {
    /**
     * When coins have duplicate symbols, such as "UNI", this visitor is used by services
     * to determine which coin is wanted for a given symbol that has multiple coins.
     * The coin wanted is the coin name, which is assumed to be unique. Thus, the coin
     * symbol/name pair should suffice for determining exactly which coin is wanted.
     * Since the exchange does not have the coin name in its API data, (only the symbol is there),
     * we simply must hard-code the values here, until the exchanges give more information in their
     * API data.
     * <p>
     * Also, some exchanges use different coin symbols than coin market cap. For example,
     * BQX on Binance is VGX on coin market cap. So, in order to fill in the market cap
     * for the BQX coin on Binance, we use this visitor to return the alternate symbol,
     * which will be used to get the market cap.
     *
     * @return the visitor to be used to determine the name of an ambiguous coin, or the
     * desired alternate symbol for a given coin.
     */
    @NotNull
    @Override
    public String getName(@NotNull String coin) {
        if (coin.equals("UNI")) {
            return "Uniswap";
        }
        if (coin.equals("HNT")) {
            return "Helium";
        }
        if (coin.equals("LINK")) {
            return "Chainlink";
        }
        if (coin.equals("CND")) {
            return "Cindicator";
        }
        return getSymbol(coin);
    }

    @NotNull
    @Override
    public String getSymbol(@NotNull String coin) {
        if (coin.equals("BQX")) {
            return "VGX";
        }
        if (coin.equals("YOYO")) {
            return "YOYOW";
        }
        if (coin.equals("PHB")) {
            return "PHX";
        }
        if (coin.equals("GXS")) {
            return "GXC";
        }
        return coin;
    }
}