package com.scanner.cryptoserver.util;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.imgscalr.Scalr.resize;

public class ImageResizer {

    public static void resizeImageToSize(InputStream uploadedInputStream, int size, String outputFile) throws IOException {
        BufferedImage image = ImageIO.read(uploadedInputStream);
        ImageIO.write(resize(image, Method.ULTRA_QUALITY, size, Scalr.OP_ANTIALIAS), "PNG", new File(outputFile));
    }

    /**
     * Image resizer utility. Used when converting coin icons to a smaller size.
     */
    public static void main(String[] args) {
        String downloadedFolder = "C:/dev/icons/coin-market-cap-downloaded/";
        String convertedFolder = "C:/dev/icons/coin-market-cap-converted3/";
        List<Path> imagePaths = new ArrayList<>();

        try {
            imagePaths = Files.list(Paths.get(downloadedFolder)).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        imagePaths.forEach(imagePath -> {
            int index = imagePath.toString().lastIndexOf("\\");
            String fileName = imagePath.toString().substring(index + 1);
            System.out.println("fileName: " + fileName);
            try (InputStream in = Files.newInputStream(imagePath)) {
                String outputImagePath = convertedFolder + fileName;
                resizeImageToSize(in, 32, outputImagePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
