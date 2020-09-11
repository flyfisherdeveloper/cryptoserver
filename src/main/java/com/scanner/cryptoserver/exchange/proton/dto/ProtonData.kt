package com.scanner.cryptoserver.exchange.proton.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/*
example:

"chainId":"71ee83bcf52142d61019d95f9cc5427ba6a0d7ff8accd9e2088ae2abeaf3d3dd",
    "chainUrl":"https://testnet.protonchain.com",
    "name":"Proton Testnet",
    "description":"Public Proton Testnet blockchain meant for develpoment and testing.",
    "iconUrl":"https://static.protonchain.com/images/eosio-tokenXPR-testnet.png", "isTestnet":true,
    "hyperionHistoryUrl":"https://testnet.protonchain.com",
    "explorerUrl":"https://proton-test.bloks.io",
    "explorerName":"Bloks.io", "resourceTokenSymbol":"SYS",
    "resourceTokenContract":"eosio.token", "systemTokenSymbol":"XPR",
    "systemTokenContract":"eosio.token",
    "createAccountPath":"/v1/chain/accounts",
    "updateAccountAvatarPath":"/v1/chain/accounts/{{account}}/avatar",
    "updateAccountNamePath":"/v1/chain/accounts/{{account}}/name",
    "exchangeRatePath":"/v1/chain/exchange-rates" }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ProtonData(val chainUrl: String, val name: String, val description: String, val iconUrl: String, val explorerUrl: String,
                      val explorerName: String, val systemTokenSymbol: String, val createAccountPath: String, val updateAccountAvatarPath: String,
                      val updateAccountNamePath: String, val exchangeRatePath: String)
