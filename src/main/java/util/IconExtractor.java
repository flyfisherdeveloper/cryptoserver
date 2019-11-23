package util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;

public class IconExtractor {

    /**
     * Extract a coin icon from a zip file.
     * @param coin the coin name.
     * @return a byte array of the png icon file.
     * @throws IOException on exception (usually when the png is not in the zip file).
     */
    public static byte[] getIconBytes(String coin) throws IOException {
        URL url = IconExtractor.class.getClassLoader().getResource("cryptocurrency-icons-master.zip");
        assert url != null;
        Path zipFile = null;
        try {
            zipFile = Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        assert zipFile != null;
        FileSystem fs = FileSystems.newFileSystem(zipFile, ClassLoader.getSystemClassLoader());
        final Path icon = fs.getPath("/cryptocurrency-icons-master/32/color/" + coin + ".png");
        byte[] bytes = Files.readAllBytes(icon);
        fs.close();
        return bytes;
    }
}
