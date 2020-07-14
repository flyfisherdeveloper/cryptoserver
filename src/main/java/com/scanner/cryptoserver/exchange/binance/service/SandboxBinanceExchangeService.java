package com.scanner.cryptoserver.exchange.binance.service;

import com.scanner.cryptoserver.exchange.service.AbstractSandboxExchangeService;
import com.scanner.cryptoserver.util.SandboxUtil;
import org.springframework.stereotype.Service;

/**
 * The purpose of a Sandbox exchange is to return data for Binance but without
 * calling the Binance exchange. This is done to avoid using Binance API quotas.
 * For example, if the client is making lots of changes and the client doesn't need up-to-date data,
 * then it would be wise to use the Sandbox data so that API calls are prevented.
 * The data in the Sandbox is actual data from a past API call to Binance that is stored in files.
 * The Sandbox data never changes - it is static.
 */
@Service(value = "sandboxBinanceService")
public class SandboxBinanceExchangeService extends AbstractSandboxExchangeService {
    private static final String sandboxName = "binance";

    public SandboxBinanceExchangeService(SandboxUtil sandboxUtil) {
        super(sandboxUtil);
    }

    protected String getSandboxName() {
        return sandboxName;
    }
}
