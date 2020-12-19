package com.scanner.cryptoserver.testutil

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapData
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapListing
import org.junit.jupiter.api.BeforeAll

abstract class AbstractIntegSetup {
    object Companion {
        private var listing: CoinMarketCapListing? = null

        /**
         * Convienience class used only for deserializing Json.
         */
        private class CoinMarketCapObj {
            val data: List<CoinMarketCapData>? = null
        }

        @BeforeAll
        fun setup() {
            val text = "/exchange/data/coinmarketcap-map.txt".getResourceAsText()
            val mapper = jacksonObjectMapper()
            val obj = mapper.readValue(text) as CoinMarketCapObj
            listing = CoinMarketCapListing().convertToCoinMarketCapListing(obj.data)
        }

        private fun String.getResourceAsText(): String {
            return object {}.javaClass.getResource(this).readText()
        }

        fun printListing() {
            println(listing)
        }

        fun getListing(): CoinMarketCapListing? {
            return listing
        }
    }

    public fun getListing(): CoinMarketCapListing? {
        Companion.setup()
        return Companion.getListing()
    }
}