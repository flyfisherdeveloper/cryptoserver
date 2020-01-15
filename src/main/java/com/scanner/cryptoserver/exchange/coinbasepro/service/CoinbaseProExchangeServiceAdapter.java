package com.scanner.cryptoserver.exchange.coinbasepro.service;

import com.scanner.cryptoserver.exchange.ExchangeService;
import com.scanner.cryptoserver.exchange.binance.us.dto.*;

import java.util.ArrayList;
import java.util.List;

public class CoinbaseProExchangeServiceAdapter implements ExchangeService {
    private final CoinbaseProExchangeService coinbaseProExchangeService;

    public CoinbaseProExchangeServiceAdapter(CoinbaseProExchangeService coinbaseProExchangeService) {
        this.coinbaseProExchangeService = coinbaseProExchangeService;
    }

    @Override
    public ExchangeInfo getExchangeInfo() {
        CoinbaseProSymbol[] coinbaseProExchangeInfo = coinbaseProExchangeService.getCoinbaseProExchangeInfo();
        //todo: adapt here
        ExchangeInfo info = new ExchangeInfo();
        List<Symbol> symbols = new ArrayList<>();
        for (CoinbaseProSymbol coinbaseProSymbol : coinbaseProExchangeInfo) {
            Symbol symbol = new Symbol();
            symbol.setSymbol(coinbaseProSymbol.getId());
            symbol.setQuoteAsset(coinbaseProSymbol.getQuote_currency());
            symbol.setBaseAsset(coinbaseProSymbol.getBase_currency());
            symbols.add(symbol);
        }
        info.setSymbols(symbols);
        return info;
    }

    @Override
    public List<CoinTicker> getTickerData(String symbol, String interval, String daysOrMonths) {
        return null;
    }

    @Override
    public CoinDataFor24Hr call24HrCoinTicker(String symbol) {
        return null;
    }

    @Override
    public List<CoinDataFor24Hr> get24HrAllCoinTicker() {
        return null;
    }

    @Override
    public byte[] getIconBytes(String coin) {
        return new byte[0];
    }

    @Override
    public List<CoinTicker> getCoinTicker(String ltcbtc, String s) {
        return null;
    }

    @Override
    public void add24HrVolumeChange(List<CoinDataFor24Hr> data) {

    }
}
