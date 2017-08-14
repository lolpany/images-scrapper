package lol.lolpany.imagesScrapper;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ImageWriter implements Runnable {

    final static String END_FILE = "lollollollollollollollollol";

    private final BlockingQueue<Pair<String, byte[]>> fileQueue;
    private final String imageLocation;
    private final int endFiles;

    public ImageWriter(BlockingQueue<Pair<String, byte[]>> fileQueue, String imageLocation,
                       int endFiles) {
        this.fileQueue = fileQueue;
        this.imageLocation = imageLocation;
//        RemoteServiceFactory remoteServiceFactory = new RemoteServiceFactory(MagentoSoapClient.getInstance());
//        ProductRemoteService productService = remoteServiceFactory.getProductRemoteService();
//        productService.setConfigurableAttributes();

        this.endFiles= endFiles;
    }

    public void run() {
        int end = 0;
        while (true) {
            try {
                Pair<String, byte[]> file = fileQueue.take();
                if (END_FILE.equals(file.getLeft())) {
                    end++;
                }
                if (end == endFiles) {
                    break;
                }
                FileUtils.writeByteArrayToFile(new File(file.getLeft()), file.getRight());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
