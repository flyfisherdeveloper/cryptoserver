package com.scanner.cryptoserver.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;

public class IconExtractor {
    private static final Logger Log = LoggerFactory.getLogger(IconExtractor.class);

    /**
     * Extract a coin icon from a zip file.
     *
     * @param coin the coin name.
     * @return a byte array of the png icon file.
     */
    public static byte[] getIconBytes(String coin) {
        ClassLoader cl = IconExtractor.class.getClassLoader();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
        Resource resource;
        byte[] bytes = null;

        //try to get the icon from the resources - known icons are stored there
        resource = resolver.getResource("classpath:icons/" + coin.toLowerCase() + ".png");
        try (InputStream stream = resource.getInputStream()) {
            if (stream == null) {
                Log.error("Cannot load input stream for png file: Coin: {}", coin);
                return null;
            }
            bytes = new byte[stream.available()];
            stream.read(bytes);
        } catch (IOException e) {
            Log.error("Cannot read bytes from png file: Coin: {}", coin, e);
        }
        return bytes;
    }
}
