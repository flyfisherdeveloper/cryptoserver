package com.scanner.cryptoserver.exchange.binance.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Symbol {
   private String symbol;
   private String baseAsset;
   private String quoteAsset;
   private String status;
}
