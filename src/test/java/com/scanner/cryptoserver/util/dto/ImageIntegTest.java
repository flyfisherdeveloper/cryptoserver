package com.scanner.cryptoserver.util.dto;

import com.scanner.cryptoserver.exchange.binance.dto.CoinDataFor24Hr;
import com.scanner.cryptoserver.exchange.binance.service.AbstractBinanceExchangeService;
import com.scanner.cryptoserver.exchange.bittrex.service.BittrexServiceImpl;
import com.scanner.cryptoserver.exchange.coinmarketcap.CoinMarketCapService;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapData;
import com.scanner.cryptoserver.exchange.coinmarketcap.dto.CoinMarketCapListing;
import com.scanner.cryptoserver.exchange.service.ExchangeService;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.imgscalr.Scalr.resize;

//Note: this test MUST be a Spring Boot Test. This is because the full coin market cap must be loaded
//in order to download missing icons.
@SpringBootTest
public class ImageIntegTest {
    @Autowired
    private AbstractBinanceExchangeService binanceService;
    @Autowired
    private AbstractBinanceExchangeService binanceUsaService;
    @Autowired
    private BittrexServiceImpl bittrexService;
    @Autowired
    private CoinMarketCapService coinMarketCapService;

    private void resizeImageToSize(InputStream uploadedInputStream, int size, String outputFile) throws IOException {
        BufferedImage image = ImageIO.read(uploadedInputStream);
        ImageIO.write(resize(image, Method.ULTRA_QUALITY, size, Scalr.OP_ANTIALIAS), "PNG", new File(outputFile));
    }

    /**
     * Image resizer utility. Used when converting coin icons to a smaller size.
     */
    private void resizeImages(String downloadedFolder, String convertedFolder) {
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
                String downloadedImagePath = downloadedFolder + fileName;
                Path downloadedImage = Paths.get(downloadedImagePath);
                Files.delete(downloadedImage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * This is a "test" to download missing icons, and convert them to a smaller size. It is used
     * periodically to repopulate icons from newly listed coins on exchanges. It really isn't a "test",
     * but is included here to run when needed.
     */
    @Disabled
    //@Test
    void findNewIconsTest() {
        String downloadedFolder = "C:/dev/icons/coin-market-cap-downloadedNew/";
        String convertedFolder = "C:/dev/icons/coin-market-cap-convertedNew/";
        List<ExchangeService> services = Arrays.asList(binanceUsaService, binanceService, bittrexService);
        List<CoinDataFor24Hr> missingIconList = new ArrayList<>();

        //here we sleep for a few seconds to ensure that the asynchronous initialization has completed
        //yes, it is a hack, but this is just a utility test that is run periodically to download icons,
        //so, whatever works is good enough
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //go through each service and get all coins
        services.forEach(service -> missingIconList.addAll(service.getMissingIcons()));

        //go through all the missing icons, and attempt to download the icon for each
        final Set<Integer> set = missingIconList.stream().map(CoinDataFor24Hr::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        CoinMarketCapListing listing = coinMarketCapService.getCoinMarketCapInfoListing(set);
        final Set<String> logoSet = listing.getData().values().stream().map(CoinMarketCapData::getLogo).collect(Collectors.toSet());
        logoSet.forEach(logo -> {
            try {
                final URL url = new URL(logo);
                final InputStream inputStream = url.openStream();
                int last = logo.lastIndexOf('/');
                String fileName = logo.substring(last + 1);
                String path = downloadedFolder + fileName;
                System.out.println("copying file:" + fileName);
                Files.copy(inputStream, Paths.get(path));
                //now attempt to resize the icons
                resizeImages(downloadedFolder, convertedFolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
