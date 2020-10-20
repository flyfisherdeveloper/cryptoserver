package com.scanner.cryptoserver.exchange.coinmarketcap;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ImageResizer {
    /**
     * Resizes an image to a absolute width and height (the image may not be
     * proportional)
     *
     * @param inputImagePath  Path of the original image
     * @param outputImagePath Path to save the resized image
     * @param scaledWidth     absolute width in pixels
     * @param scaledHeight    absolute height in pixels
     * @throws IOException
     */
    public static void resize(String inputImagePath,
                              String outputImagePath, int scaledWidth, int scaledHeight) throws IOException {
        // reads input image
        File inputFile = new File(inputImagePath);
        BufferedImage inputImage = ImageIO.read(inputFile);

        // creates output image
        BufferedImage outputImage = new BufferedImage(scaledWidth, scaledHeight, inputImage.getType());

        // scales the input image to the output image
        Graphics2D g2d = outputImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        // extracts extension of output file
        String formatName = outputImagePath.substring(outputImagePath.lastIndexOf(".") + 1);

        // writes to output file
        ImageIO.write(outputImage, formatName, new File(outputImagePath));
    }

    /**
     * Resizes an image by a percentage of original size (proportional).
     *
     * @param inputImagePath  Path of the original image
     * @param outputImagePath Path to save the resized image
     * @param percent         a double number specifies percentage of the output image
     *                        over the input image.
     * @throws IOException
     */
    public static void resize(String inputImagePath, String outputImagePath, double percent) throws IOException {
        File inputFile = new File(inputImagePath);
        BufferedImage inputImage = ImageIO.read(inputFile);
        int scaledWidth = (int) (inputImage.getWidth() * percent);
        int scaledHeight = (int) (inputImage.getHeight() * percent);
        resize(inputImagePath, outputImagePath, scaledWidth, scaledHeight);
    }

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
                //double percent = 0.5;
                //ImageResizer.resize(inputImagePath.toString(), outputImagePath, percent);
                int scaledWidth = 32;
                int scaledHeight = 32;
                //ImageResizer.resize(inputImagePath.toString(), outputImagePath, scaledWidth, scaledHeight);
                resizeImage(in, outputImagePath, scaledWidth, scaledHeight);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        /*
        imagePaths.forEach(file -> {
            try {
                // resize to a fixed width (not proportional)
                //int scaledWidth = 32;
                //int scaledHeight = 32;
                //ImageResizer.resize(inputImagePath, outputImagePath1, scaledWidth, scaledHeight);

                // resize smaller by 50%
                double percent = 0.5;
                ImageResizer.resize(inputImagePath, outputImagePath, percent);
            } catch (IOException ex) {
                System.out.println("Error resizing the image.");
                ex.printStackTrace();
            }
        });

         */
    }
}
