package com.scanner.cryptoserver.exchange.bittrex.service

import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr
import com.scanner.cryptoserver.exchange.service.AbstractSandboxExchangeService
import com.scanner.cryptoserver.util.SandboxUtil
import org.springframework.stereotype.Service

/**
 * The purpose of a Sandbox exchange is to return data for an exchange but without
 * calling the exchange. This is done to avoid using the exchange API quotas.
 * For example, if the client is making lots of changes and the client doesn't need up-to-date data,
 * then it would be wise to use the Sandbox data so that API calls are prevented.
 * The data in the Sandbox is actual data from a past API call to the exchange that is stored in files.
 * The Sandbox data never changes - it is static.
 */
@Service(value = "sandboxBittrexExchangeService")
class SandboxBittrexExchangeService(sandboxUtil: SandboxUtil) : AbstractSandboxExchangeService(sandboxUtil) {
    private val sandboxName = "bittrex"

    override fun getSandboxName(): String {
        return sandboxName
    }

    override fun get24HrAllCoinTicker(): List<CoinDataFor24Hr> {
        return getDataList<CoinDataFor24Hr>(getDataName("24HourTicker"), CoinDataFor24Hr::class.java)
    }
}