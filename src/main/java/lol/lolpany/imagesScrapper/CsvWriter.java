package lol.lolpany.imagesScrapper;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class CsvWriter implements Runnable {

    private final List<BlockingQueue<StringBuilder>> inputQueues;
    private int batchNumber;

    public CsvWriter(List<BlockingQueue<StringBuilder>> inputQueues) {
        this.inputQueues = inputQueues;
        this.batchNumber = 0;
    }

    @Override
    public void run() {
        while (true) {
            for (int i = 0; i < inputQueues.size(); i++) {
                try {
                    StringBuilder csvPart = inputQueues.get(i).poll(1, TimeUnit.SECONDS);
                    if (csvPart != null) {
                        writeCsv(csvPart);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void writeCsv(StringBuilder csv) {
        try {
            FileUtils.writeStringToFile(new File("D:\\buffer\\magmi\\lol" + batchNumber + ".csv"),
                    "sku,image,small_image,thumbnail\n" + csv.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        batchNumber++;
    }

}
