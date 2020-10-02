package com.scanner.cryptoserver.exchange.proton

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.scanner.cryptoserver.exchange.proton.dto.ProtonData
import com.scanner.cryptoserver.util.UrlReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service


@Service
class ProtonApiServiceImpl(private val urlReader: UrlReader) {
    @Value("\${exchanges.proton.api}")
    private val protonApiUrl: String? = null

    fun getInfo(): ProtonData {
        val results: String = if (protonApiUrl == null) {
            urlReader.readFromUrl()
        } else {
            urlReader.readFromUrl(protonApiUrl)
        }
        val mapper = jacksonObjectMapper()
        return mapper.readValue(results) as ProtonData
    }
}