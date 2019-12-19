package com.scanner.cryptoserver.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;

public class IconExtractor {
    private static final Logger Log = LoggerFactory.getLogger(IconExtractor.class);

    /**
     * Extract a coin icon from a zip file.
     *
     * @param coin the coin name.
     * @return a byte array of the png icon file.
     */
    public static byte[] getIconBytes(String coin) {
        URL url = IconExtractor.class.getClassLoader().getResource("cryptocurrency-icons-master.zip");
        assert url != null;
        Path zipFile;

        try {
            zipFile = Paths.get(url.toURI());
        } catch (Exception e) {
            Log.error("Cannot open icons zip file. Coin: {}", coin, e);
            return null;
        }

        FileSystem fs;
        byte[] bytes = null;
        try {
            fs = FileSystems.newFileSystem(zipFile, ClassLoader.getSystemClassLoader());
            final Path icon = fs.getPath("/cryptocurrency-icons-master/32/color/" + coin + ".png");
            bytes = Files.readAllBytes(icon);
            fs.close();
        } catch (IOException e) {
            Log.error("Cannot extract png from icons zip file. Coin: {}", coin, e);
        }
        return bytes;
    }
}
