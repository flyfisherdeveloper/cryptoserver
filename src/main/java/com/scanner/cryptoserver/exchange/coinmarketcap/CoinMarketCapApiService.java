package com.scanner.cryptoserver.exchange.coinmarketcap;

import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapListing;
import org.apache.http.NameValuePair;

import java.util.List;

public interface CoinMarketCapApiService {
    CoinMarketCapListing getCoinMarketCapMap();

    String makeExchangeInfoApiCall(List<NameValuePair> paratmers);

    String makeExchangeQuotesApiCall(List<NameValuePair> parameters);
}
