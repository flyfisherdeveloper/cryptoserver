package com.scanner.cryptoserver.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        //try to get the icon from the resources - known icons are stored there
        InputStream stream = IconExtractor.class.getClassLoader().getResourceAsStream("icons/" + coin + ".png");
        if (stream == null) {
            return null;
        }
        byte[] bytes = null;
        try {
            bytes = new byte[stream.available()];
            stream.read(bytes);
        } catch (IOException e) {
            Log.error("Cannot read bytes from png file: Coin: {}", coin, e);
        }
        return bytes;
    }
}
