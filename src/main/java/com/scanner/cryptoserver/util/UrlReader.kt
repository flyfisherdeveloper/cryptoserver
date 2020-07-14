package com.scanner.cryptoserver.util

import java.net.URL

/**
 * Class that allows a mock url reader to be used if not using an exchange api, for testing purposes.
 */
interface UrlReader {

    fun readFromUrl(address: String): String {
        return URL(address).readText()
    }

    fun readFromUrl(): String
}