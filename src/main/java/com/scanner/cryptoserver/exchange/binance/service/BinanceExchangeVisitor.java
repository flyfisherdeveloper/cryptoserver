package com.scanner.cryptoserver.exchange.binance.service;

import com.scanner.cryptoserver.exchange.service.ExchangeVisitor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class BinanceExchangeVisitor implements ExchangeVisitor {
    private final Map<String, String> nameMap = Map.ofEntries(Map.entry("UNI", "Uniswap"), Map.entry("HNT", "Helium"), Map.entry("LINK", "Chainlink"),
            Map.entry("CND", "Cindicator"), Map.entry("HOT", "Holo"), Map.entry("COMP", "Compound"));

    private final Map<String, String> symbolMap = Map.ofEntries(Map.entry("BQX", "VGX"), Map.entry("YOYO", "YOYOW"), Map.entry("PHB", "PHX"),
            Map.entry("GXS", "GXC"), Map.entry("WNXM", "NXM"), Map.entry("GLM", "GNT"));

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
        return Optional.ofNullable(nameMap.get(coin)).orElseGet(() -> getSymbol(coin));
    }

    @NotNull
    @Override
    public String getSymbol(@NotNull String coin) {
        return Optional.ofNullable(symbolMap.get(coin)).orElse(coin);
    }
}