package com.scanner.cryptoserver.util

import org.springframework.stereotype.Service

@Service
class UrlReaderImpl : UrlReader {
    //this method will be implemented (mocked) for testing, etc.
    override fun readFromUrl(): String {
        return ""
    }
}