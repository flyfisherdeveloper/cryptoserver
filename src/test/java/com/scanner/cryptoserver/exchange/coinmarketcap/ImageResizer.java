package com.scanner.cryptoserver.exchange.coinmarketcap;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ImageResizer {

    private static void resizeImage(InputStream uploadedInputStream, String fileName, int width, int height) throws IOException {
        BufferedImage image = ImageIO.read(uploadedInputStream);
        Image originalImage = image.getScaledInstance(width, height, Image.SCALE_DEFAULT);

        int type = ((image.getType() == 0) ? BufferedImage.TYPE_INT_ARGB : image.getType());
        BufferedImage resizedImage = new BufferedImage(width, height, type);

        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, width, height, null);
        g2d.setComposite(AlphaComposite.Src);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.dispose();

        FileOutputStream fos = new FileOutputStream(fileName);
        ImageIO.write(resizedImage, fileName.split("\\.")[1], fos);
        fos.close();
    }

    /**
     * Test resizing images
     */
    public static void main(String[] args) {
        String downloadedFolder = "C:/dev/icons/coin-market-cap-downloaded/";
        String convertedFolder = "C:/dev/icons/coin-market-cap-converted/";
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
                int scaledWidth = 32;
                int scaledHeight = 32;
                resizeImage(in, outputImagePath, scaledWidth, scaledHeight);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
