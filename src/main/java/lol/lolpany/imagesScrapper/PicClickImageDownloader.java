package lol.lolpany.imagesScrapper;

import com.google.code.magja.model.product.Product;
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
import java.nio.file.Files;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static lol.lolpany.imagesScrapper.ImageWriter.END_FILE;
import static lol.lolpany.imagesScrapper.Main.SIZE_LIMIT_TO_FILTER_OUT_BAD_AND_MANUFACTURER;
import static lol.lolpany.imagesScrapper.Product2.END_QUEUE;
import static org.apache.commons.io.IOUtils.toByteArray;

public class PicClickImageDownloader implements Runnable {

    final ProductRemoteService productService;
    final BlockingQueue<Product2> inputQueue;
//    final BlockingQueue<Product2> outputQueue;
    final int n;
    final BlockingQueue<Pair<String, byte[]>> fileQueue;
    final String imagesRoot;
    final String magmiDir;
    private final int downloaderIndex;
    final int dumpEvery;


    PicClickImageDownloader(ProductRemoteService productService, BlockingQueue<Product2> inputQueue,
                            BlockingQueue<Product2> outputQueue, int n, BlockingQueue<Pair<String, byte[]>> fileQueue,
                            String imagesRoot, String magmiDir, int downloaderIndex,
                            int dumpEvery) {
        this.productService = productService;
        this.inputQueue = inputQueue;
//        this.outputQueue = outputQueue;
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
                Product2 productToDump = inputQueue.take();
                if (productToDump .equals( END_QUEUE)) {
                    break;
                }
                System.out.println(productToDump.upc);
                Document doc = Jsoup.connect("https://picclick.com/?q=" + URLEncoder.encode(
                        productToDump.upc, "UTF-8")).get();
                Elements productLinks = doc.select("ul.items > li > a");
                if (!"NULL".equals(productToDump.upc) && !productLinks.isEmpty()) {
                    for (Element productHrefEl : productLinks) {
                        String productHref = productHrefEl.attr("href");
//                    int i = 0;

                        Element image = Jsoup.connect("https://picclick.com" + productHref).get().select(".photos img").get(0);
                        String imageSrc = image.attr("src");
                        // for first image
                        imageSrc = imageSrc.replace("/l400/", "/w1600/");
                        // for subsequent images
                        imageSrc = imageSrc.replace("_1.", "_57.");
                        URL url = new URL(imageSrc);
                        URLConnection con = url.openConnection();
                        con.setConnectTimeout(100000);
                        con.setReadTimeout(100000);
                        String imageExtension = imageSrc.substring(imageSrc.lastIndexOf(".") + 1);
//                        FileUtils.writeByteArrayToFile(new File("D:\\buffer\\jopa\\" + productToDump.id + "_" /*+ i*/ + imageExtension), );
//                        i++;
//                    setProductImage(productService, productToDump.getLeft(), productToDump.getRight(), imageExtension,
//                            toByteArray(con.openStream()));
                        String sku = productToDump.sku;
                        byte[] imageBytes = toByteArray(con.getInputStream());
                        if (imageBytes.length < SIZE_LIMIT_TO_FILTER_OUT_BAD_AND_MANUFACTURER) {
                            continue;
                        }
                        System.out.println(imagesRoot + File.separator + sku
                                        + "." + imageExtension);
                        result.append("\n" + sku + "," + sku + "."  + imageExtension + ","
                                + sku + "."  + imageExtension
                                + "," + sku + "."  + imageExtension);
                        i++;
                        if (i == dumpEvery ) {
                            i=0;
                            try {
                                FileUtils.writeStringToFile(new File(magmiDir + File.separator+ "picclick"
                                        + downloaderIndex+"-"+batchNumber  +".csv"), result.toString(), StandardCharsets.UTF_8);
                                batchNumber++;
                                result.setLength(0);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                        fileQueue.put(new ImmutablePair<>(imagesRoot + File.separator + sku
                                + "." + imageExtension, imageBytes));

                        break;
                    }
                } else {
//                    outputQueue.put(productToDump);
                }
            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            FileUtils.writeStringToFile(new File(magmiDir + File.separator+ "picclick"+ downloaderIndex+".csv"), result.toString(), StandardCharsets.UTF_8);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        try {
//            outputQueue.put(END_QUEUE);
            fileQueue.put(new ImmutablePair<>(END_FILE, null));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
