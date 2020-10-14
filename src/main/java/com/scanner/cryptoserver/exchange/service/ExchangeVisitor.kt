package com.scanner.cryptoserver.exchange.service

/**
 * Interface used to extract information that is coupled with an exchange, such as which coin an
 * exchange wants given a symbol. For example, the coin symbol "UNI" might exist for multiple coins
 * such as "Universe" and "Uniswap". This visitor allows a service to "visit" an exchange for information
 * without being coupled with the exchange service.
 */
interface ExchangeVisitor {
    /**
     * Determine which exact coin is needed for a given coin symbol, such as "UNI". Will
     * return the coin name based on what the exchange is expecting, such as "Uniswap" for "UNI", given
     * that "UNI" represents multiple coins in the coin market cap data, such as "Universe" and "Uniswap".
     * This allows a unique representation (i.e. "UNI/Uniswap") for the ambiguous coin symbol ("UNI").
     */
    fun getName(coin: String): String

    /**
     * Determine which symbol, or base asset is needed for a coin symbol.
     * Sometimes a coin symbol on the exchange is different in the coin market cap data.
     * For example, BQX on Binance is VGX on coin market cap. In order to get the market
     * cap data, we need a way to visit the exchange to get the alternate symbol.
     */
    fun getSymbol(coin: String): String
}