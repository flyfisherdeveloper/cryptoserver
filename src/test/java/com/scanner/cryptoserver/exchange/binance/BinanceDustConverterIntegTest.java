package com.scanner.cryptoserver.exchange.binance;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

public class BinanceDustConverterIntegTest {

    private LocalDateTime parseDate(String[] strs) {
        String dateStr = strs[0] + "T" + strs[1];
        return LocalDateTime.parse(dateStr);
    }

    @Test
    void testDustConverter() throws URISyntaxException, IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource("files/dust.txt");
        Path path = null;
        if (url != null) {
            URI uri = url.toURI();
            path = Paths.get(uri);
        }
        //Date	Coin	Amount	Fee(BNB)	Converted BNB
        if (path != null) {
            Stream<String> lines = Files.lines(path);
            lines.forEach(line -> {
                String[] strs = line.split("\\s+");
                String date = strs[0] + " " + strs[1];
                Instant instant = LocalDateTime.of(2018, Month.MAY, 14, 22, 34, 22).toInstant(ZoneOffset.UTC);
                LocalDateTime localDateTime = parseDate(strs);
                System.out.println("local date time: " + localDateTime);
                instant = localDateTime.toInstant(ZoneOffset.UTC);
                System.out.println("instant: " + instant);
                //System.out.println("date: " + date);
                for (String str : strs) {
                    //System.out.println(str);
                }
            });
            //def nextTime = LocalDateTime.of(2018, Month.MAY, 14, 22, 34, 22).toInstant(ZoneOffset.UTC).toEpochMilli()
        }
        File file = new File(url.toString());
    }
}
