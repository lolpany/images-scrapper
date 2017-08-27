package lol.lolpany.imagesScrapper;

import com.google.code.magja.service.product.ProductRemoteService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Thread.sleep;
import static lol.lolpany.imagesScrapper.ImageWriter.END_FILE;
import static lol.lolpany.imagesScrapper.Main.SIZE_LIMIT_TO_FILTER_OUT_BAD_AND_MANUFACTURER;
import static lol.lolpany.imagesScrapper.Product2.END_QUEUE;
import static lol.lolpany.imagesScrapper.Utils.identifyImageExtension;
import static org.apache.commons.io.IOUtils.toByteArray;

public class GoogleImageDownloader implements Runnable {

    private static final Pattern META_DIV_URL_PATTERN = Pattern.compile(".*\"ou\":\"(.+?)[?\"].*");

    final ProductRemoteService productService;
    final BlockingQueue<Product2> inputQueue;
    final BlockingQueue<Pair<String, byte[]>> fileQueue;
    final String imagesRoot;
    final String magmiDir;
    final int downloaderIndex;
    final int dumpEvery;
    final BlockingQueue<StringBuilder> csvWriterQueue;


    GoogleImageDownloader(ProductRemoteService productService, BlockingQueue<Product2> inputQueue,
                          BlockingQueue<Pair<String, byte[]>> fileQueue, String imagesRoot,
                          String magmiDir, int downloaderIndex, int dumpEvery,
                          BlockingQueue<StringBuilder> csvWriterQueue) {
        this.productService = productService;
        this.inputQueue = inputQueue;
        this.fileQueue = fileQueue;
        this.imagesRoot = imagesRoot;
        this.magmiDir = magmiDir;
        this.downloaderIndex = downloaderIndex;
        this.dumpEvery = dumpEvery;
        this.csvWriterQueue = csvWriterQueue;
    }

    public void run() {
        StringBuilder result = new StringBuilder("sku,image,small_image,thumbnail");
        int i = 0;
        while (true) {
            try {
                Product2 productToDump = inputQueue.take();

                if (productToDump.equals(END_QUEUE)) {
                    break;
                }
                if (productToDump.name.contains("SW FOUND")) {
                    continue;
                }
                Document doc = Jsoup.connect("https://www.google.ru/search?newwindow=1&tbm=isch&sa=1&q="
                        + URLEncoder.encode(productToDump.name, "UTF-8")).get();
                Elements metaDivs = doc.select("#isr_mc div.rg_meta");


                Element image = metaDivs.get(findLargestImageIndex(doc, 12));

                if (!"NULL".equals(productToDump.name) && !metaDivs.isEmpty()) {
                    if (image != null) {
                        String metaDivText = image.ownText();
                        Matcher urlMatcher = META_DIV_URL_PATTERN.matcher(metaDivText);
                        urlMatcher.find();
                        String imageSrc = urlMatcher.group(1);
                        URL url = new URL(imageSrc);
                        URLConnection con = url.openConnection();
                        con.setConnectTimeout(100000);
                        con.setReadTimeout(100000);
                        byte[] imageBytes = toByteArray(con.getInputStream());
                        String imageExtension = identifyImageExtension(imageBytes);
                        if (imageExtension != null) {
                            String sku = productToDump.sku;
                            if (imageBytes.length < SIZE_LIMIT_TO_FILTER_OUT_BAD_AND_MANUFACTURER) {
                                continue;
                            }
                            result.append("\n" + sku + "," + sku + "." + imageExtension + ","
                                    + sku + "." + imageExtension
                                    + "," + sku + "." + imageExtension);
                            i++;
                            if (i == dumpEvery) {
                                i = 0;
                                csvWriterQueue.put(result);
                                result = new StringBuilder("");
                            }
                            fileQueue.put(new ImmutablePair<>(imagesRoot + "\\" + sku + "." + imageExtension,
                                    imageBytes));
                            sleep(2000 + Math.round(Math.random()) * 10000);
                        }
                    }
                }

            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try

        {
            FileUtils.writeStringToFile(new File(magmiDir + File.separator +
                    "google" + downloaderIndex + ".csv"), result.toString(), StandardCharsets.UTF_8);
        } catch (
                IOException e1)

        {
            e1.printStackTrace();
        }
        try {

            fileQueue.put(new ImmutablePair<>(END_FILE, null));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private int findLargestImageIndex(Document doc, int limit) {
        int maxImageIndex = 0;
        int maxImageSize = 0;
        Elements elements = doc.select("span.rg_ilmn");
        for (int i = 0; i < limit; i++) {
            String[] contentParts = elements.get(i).ownText().split(" ");
            String[] dimensions = contentParts[0].split("×");
            int size = Integer.parseInt(dimensions[0].substring(0, dimensions[0].length() - 1))
                    * Integer.parseInt(dimensions[1].substring(1));
            if (size > maxImageSize) {
                maxImageSize = size;
                maxImageIndex = i;
            }
        }
        return maxImageIndex;
    }
}
