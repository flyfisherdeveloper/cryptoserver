package com.scanner.cryptoserver.exchange.proton

import com.scanner.cryptoserver.util.UrlReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service


@Service
class ProtonApiServiceImpl(private val urlReader: UrlReader) {
    @Value("\${exchanges.proton.api}")
    private val protonApiUrl: String? = null

    fun getInfo() {
        val results: String = if (protonApiUrl == null) {
            urlReader.readFromUrl()
        } else {
            urlReader.readFromUrl(protonApiUrl)
        }
        println(results)
    }
}