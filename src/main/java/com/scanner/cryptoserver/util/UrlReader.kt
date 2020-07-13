package com.scanner.cryptoserver.util

import org.springframework.stereotype.Service
import java.net.URL

/**
 * Class that allows a mock url reader to be used if not using an exchange api, for testing purposes.
 */
@Service
interface UrlReader {

    fun readFromUrl(address: String): String {
        return URL(address).readText()
    }

    fun readFromUrl(): String
}