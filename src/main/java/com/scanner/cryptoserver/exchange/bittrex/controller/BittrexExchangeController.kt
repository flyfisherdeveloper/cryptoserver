package com.scanner.cryptoserver.exchange.bittrex.controller

import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr
import com.scanner.cryptoserver.exchange.bittrex.service.BittrexServiceImpl
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
//@CrossOrigin(origins = ["https://develop.d2vswqrfiywrsc.amplifyapp.com"])
@CrossOrigin(origins = ["http://localhost:3000"])
@RequestMapping("api/v1/bittrex")
class BittrexExchangeController(val service: BittrexServiceImpl) {

    /**
     * Gets all the coins and 24-hour data on the exchange.
     * NOTE: This call has the heaviest "weight" of all exchange calls: Use sparingly!
     *
     * @return a list of all coins on the exchange over the past 24 hours.
     */
    @GetMapping(value = ["/24HourTicker"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAll24HourTicker(): List<CoinDataFor24Hr> {
        return service.get24HrAllCoinTicker()
    }
}