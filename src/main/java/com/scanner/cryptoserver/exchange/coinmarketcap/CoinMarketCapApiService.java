package com.scanner.cryptoserver.exchange.coinmarketcap;

import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapMap;
import org.apache.http.NameValuePair;

import java.util.List;

public interface CoinMarketCapApiService {
    CoinMarketCapMap getCoinMarketCapMap();

    String makeExchangeInfoApiCall(List<NameValuePair> paratmers);

    String makeExchangeQuotesApiCall(List<NameValuePair> parameters);
}
