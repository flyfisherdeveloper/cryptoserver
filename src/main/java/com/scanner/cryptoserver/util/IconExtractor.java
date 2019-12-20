package com.scanner.cryptoserver.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.*;

public class IconExtractor {
    private static final Logger Log = LoggerFactory.getLogger(IconExtractor.class);

    private static byte[] getFromResources(String coin) {
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

    /**
     * Extract a coin icon from a zip file.
     *
     * @param coin the coin name.
     * @return a byte array of the png icon file.
     */
    public static byte[] getIconBytes(String coin) {
        //try to get the icon from the resources - known icons are stored there
        byte[] pngBytes = getFromResources(coin);
        if (pngBytes != null && pngBytes.length > 0) {
            return pngBytes;
        }
        //didn't find it - so try to extract it from the large zip file
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
            //todo: the following is temporary: write the file to a resource folder so that we have it available
            OutputStream stream = new FileOutputStream("C:\\dev\\myscanner\\src\\main\\resources\\icons\\" + coin + ".png");
            Files.copy(icon, stream);
            bytes = Files.readAllBytes(icon);
            fs.close();
            stream.close();
        } catch (IOException e) {
            Log.error("Cannot extract png from icons zip file. Coin: {}", coin, e);
        }
        return bytes;
    }
}
