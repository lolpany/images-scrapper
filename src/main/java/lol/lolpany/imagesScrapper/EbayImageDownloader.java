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
import java.util.regex.Pattern;

import static lol.lolpany.imagesScrapper.ImageWriter.END_FILE;
import static lol.lolpany.imagesScrapper.Main.*;
import static lol.lolpany.imagesScrapper.Product2.END_QUEUE;
import static org.apache.commons.io.IOUtils.toByteArray;

public class EbayImageDownloader implements Runnable {

    private static final Pattern META_DIV_URL_PATTERN = Pattern.compile("&quot;preview&quot;:\\[\\{&quot;url&quot;:&quot;(.*?)&quot;");

    final ProductRemoteService productService;
    final BlockingQueue<Product2> inputQueue;
    final BlockingQueue<Pair<String, byte[]>> fileQueue;
    final String imagesRoot;
    final String magmiDir;
    final int downloaderIndex;
    final int dumpEvery;
    final BlockingQueue<StringBuilder> csvWriterQueue;


    EbayImageDownloader(ProductRemoteService productService, BlockingQueue<Product2> inputQueue,
                        BlockingQueue<Pair<String, byte[]>> fileQueue, String imagesRoot,
                        String magmiDir, int downloaderIndex, int dumpEvery, BlockingQueue<StringBuilder> csvWriterQueue) {
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
        StringBuilder result = new StringBuilder("");
        int i = 0;
        int batchNumber = 0;
        while (true) {
            try {
                Product2 productToDump = inputQueue.take();

                if (productToDump.equals(END_QUEUE)) {
                    break;
                }
                Document doc = Jsoup.connect("https://www.ebay.com/sch/i.html?_nkw="
                        + URLEncoder.encode(productToDump.name, "UTF-8")).get();
                Elements metaDivs = doc.select("li.sresult div img");
                if (!"NULL".equals(productToDump.name) && !metaDivs.isEmpty()) {
                    int imageSize = STARTING_IMAGE_SIZE;
                    boolean downed= false;
                    while (imageSize > MIN_IMAGE_SIZE && !downed) {
                        for (Element metaDiv : metaDivs) {
                            String imageSrc = metaDiv.attr("src");
                            imageSrc = imageSrc.replace("s-l225", "s-l1600");
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
                            if (imageBytes.length <= imageSize) {
                                continue;
                            }
                            result.append("\n" + sku + "," + sku + "." + imageExtension + ","
                                    + sku + "." + imageExtension
                                    + "," + sku + "." + imageExtension);
                            i++;
                            if (i == dumpEvery) {
                                i = 0;
//                                FileUtils.writeStringToFile(new File(magmiDir + File.separator+ "ebay"
//                                        + downloaderIndex+"-"+batchNumber  +".csv"), result.toString(), StandardCharsets.UTF_8);
                                csvWriterQueue.put(result);
//                                batchNumber++;
                                result = new StringBuilder("");
                            }
                            fileQueue.put(new ImmutablePair<>(imagesRoot + "\\" + sku + "." + imageExtension,
                                    imageBytes));
                            downed = true;

                            break;
                        }
                        imageSize -= IMAGE_SIZE_STEP;
                    }
                }

            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
//            FileUtils.writeStringToFile(new File(magmiDir + File.separator+
//                    "ebay"+ downloaderIndex+".csv"), result.toString(), StandardCharsets.UTF_8);
            csvWriterQueue.put(result);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            fileQueue.put(new ImmutablePair<>(END_FILE, null));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
