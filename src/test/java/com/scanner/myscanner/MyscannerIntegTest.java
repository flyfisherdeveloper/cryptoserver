package com.scanner.myscanner;

import com.scanner.myscanner.exchange.binance.us.dto.CoinTicker;
import com.scanner.myscanner.exchange.binance.us.dto.ExchangeInfo;
import com.scanner.myscanner.exchange.binance.us.dto.Symbol;
import com.scanner.myscanner.exchange.binance.us.service.ExchangeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class MyscannerIntegTest {
	@Autowired
	private ExchangeService exchangeService;

	@Test
	public void testAllUsdSymbols() {
		ExchangeInfo exchangeInfo = exchangeService.getExchangeInfo();
		List<Symbol> usdSymbols = exchangeInfo.getSymbols().stream()
				.filter(s -> s.getQuoteAsset().equalsIgnoreCase("USD"))
				.collect(Collectors.toList());
		usdSymbols.forEach(u -> System.out.println(u.getSymbol()));
		boolean allMatch = usdSymbols.stream().allMatch(s -> s.getSymbol().endsWith("USD"));
		assertTrue(allMatch);
	}

	@Test
	public void testCoinTicker() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime from = now.minusHours(13);
		long nowLong = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		long fromLong = from.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

		LocalDateTime fromTime =
				LocalDateTime.ofInstant(Instant.ofEpochMilli(fromLong),
						TimeZone.getDefault().toZoneId());
		System.out.println(fromTime);
		LocalDateTime toTime =
				LocalDateTime.ofInstant(Instant.ofEpochMilli(nowLong),
						TimeZone.getDefault().toZoneId());
		System.out.println(toTime);

		List<CoinTicker> tickers = exchangeService.getCoinTicker("LTCBTC", "12h", fromLong, nowLong);
		for (CoinTicker ticker : tickers) {
			LocalDateTime openTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(ticker.getOpenTime()),
							TimeZone.getDefault().toZoneId());
			LocalDateTime closeTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(ticker.getCloseTime()),
					TimeZone.getDefault().toZoneId());
			System.out.println("open time: " + openTime);
			System.out.println("close time: " + closeTime);
			System.out.println(ticker);
			System.out.println();
		}
	}
}
