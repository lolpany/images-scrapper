package lol.lolpany.imagesScrapper;

import com.google.code.magja.service.product.ProductRemoteService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lol.lolpany.imagesScrapper.ImageWriter.END_FILE;
import static lol.lolpany.imagesScrapper.Main.SIZE_LIMIT_TO_FILTER_OUT_BAD_AND_MANUFACTURER;
import static lol.lolpany.imagesScrapper.Product2.END_QUEUE;
import static org.apache.commons.io.IOUtils.toByteArray;

public class YandexImageDownloader implements Runnable {

    private static final Pattern META_DIV_URL_PATTERN = Pattern.compile("&quot;preview&quot;:\\[\\{&quot;url&quot;:&quot;(.*?)&quot;");

    final ProductRemoteService productService;
    final BlockingQueue<Product2> inputQueue;
    final int n;
    final BlockingQueue<Pair<String, byte[]>> fileQueue;
    final String imagesRoot;
    final String magmiDir;
    final int downloaderIndex;
    final int dumpEvery;


    YandexImageDownloader(ProductRemoteService productService, BlockingQueue<Product2> inputQueue,
                          int n, BlockingQueue<Pair<String, byte[]>> fileQueue, String imagesRoot,
                          String magmiDir, int downloaderIndex, int dumpEvery) {
        this.productService = productService;
        this.inputQueue = inputQueue;
        this.n = n;
        this.fileQueue = fileQueue;
        this.imagesRoot = imagesRoot;
        this.magmiDir = magmiDir;
        this.downloaderIndex = downloaderIndex;
        this.dumpEvery = dumpEvery;
    }

    public void run() {
        StringBuilder result = new StringBuilder("sku,image,small_image,thumbnail");
        int i = 0;
        int batchNumber = 0;
        while (true) {
            try {
                Product2 productToDump = inputQueue.poll(5, TimeUnit.MINUTES);

                if (productToDump == null) {
                    break;
                }
                Document doc = Jsoup.connect("https://yandex.ru/images/search?text="
                        + URLEncoder.encode(productToDump.name, "UTF-8")).get();
                Elements metaDivs = doc.select("div.serp-item_type_search");
                if (!"NULL".equals(productToDump.name) && !metaDivs.isEmpty()) {
                    for (Element metaDiv : metaDivs) {
                        String metaDivText = metaDiv.outerHtml();
                        Matcher urlMatcher = META_DIV_URL_PATTERN.matcher(metaDivText);
                        urlMatcher.find();
                        String imageSrc = urlMatcher.group(1);
                        URL url = new URL(imageSrc);
                        URLConnection con = url.openConnection();
                        con.setConnectTimeout(100000);
                        con.setReadTimeout(100000);
                        String imageExtension = imageSrc.substring(imageSrc.lastIndexOf(".") + 1);
//                i++;
//                    setProductImage(productService, productToDump.getLeft(), productToDump.getRight(), imageExtension,
//                            toByteArray(con.openStream()));
                        String sku = productToDump.sku;
                        byte[] imageBytes = toByteArray(con.getInputStream());
                        if (imageBytes.length < SIZE_LIMIT_TO_FILTER_OUT_BAD_AND_MANUFACTURER) {
                            continue;
                        }
                        result.append("\n" + sku + "," + sku + "."  + imageExtension+ ","
                                + sku + "."  + imageExtension
                                + "," + sku + "."  + imageExtension);
                        i++;
                        if (i == dumpEvery ) {
                            i=0;
                            i=0;
                            try {
                                FileUtils.writeStringToFile(new File(magmiDir + File.separator+ "yandex"
                                        + downloaderIndex+"-"+batchNumber  +".csv"), result.toString(), StandardCharsets.UTF_8);
                                batchNumber++;
                                result.setLength(0);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                        fileQueue.put(new ImmutablePair<>(imagesRoot + "\\" + sku + "." + imageExtension,
                                imageBytes));

                        break;
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
            FileUtils.writeStringToFile(new File(magmiDir + File.separator+
                    "google"+ downloaderIndex+".csv"), result.toString(), StandardCharsets.UTF_8);
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
}
