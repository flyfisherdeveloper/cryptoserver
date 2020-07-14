package com.scanner.cryptoserver.exchange.bittrex.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class BittrexTicker {
    var symbol = ""
    var lastTradeRate = 0.0
}
