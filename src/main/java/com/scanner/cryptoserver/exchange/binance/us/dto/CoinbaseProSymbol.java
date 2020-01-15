package com.scanner.cryptoserver.exchange.binance.us.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoinbaseProSymbol {
   private String id;
   private String base_currency;
   private String quote_currency;
}
