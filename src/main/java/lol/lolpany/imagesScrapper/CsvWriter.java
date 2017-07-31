package lol.lolpany.imagesScrapper;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class CsvWriter implements Runnable {

    private final List<BlockingQueue<StringBuilder>> inputQueues;
    private final List<List<StringBuilder>> csvParts;
    private int batchNumber;

    public CsvWriter(List<BlockingQueue<StringBuilder>> inputQueue, int imageWritersNubmer) {
        this.inputQueues = inputQueue;
        this.csvParts = new ArrayList<>(imageWritersNubmer);
        for (int i = 0 ; i < imageWritersNubmer; i++){
            csvParts.add(new ArrayList<>());
        }
        this.batchNumber = 0;
    }

    @Override
    public void run() {
        while (true) {
            for (int i = 0; i < inputQueues.size(); i++) {
                try {
                    StringBuilder csvPart = inputQueues.get(i).poll(1, TimeUnit.SECONDS);
                    if (csvPart != null) {
                        csvParts.get(i).add(csvPart);
                        tryMerge();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void tryMerge() {
        boolean isAllParts = true;
        for (List<StringBuilder> csvPart: csvParts) {
            if (csvPart.size()<1) {
                isAllParts = false;
                break;
            }
        }
        if (isAllParts){
            StringBuilder csv = new StringBuilder();
            for (List<StringBuilder> csvPart: csvParts) {
                csv.append(csvPart.get(0));
                csvPart.remove(0);
            }
            try {
                FileUtils.writeStringToFile(new File("D:\\buffer\\magmi\\lol" + batchNumber + ".csv"), csv.toString(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
            batchNumber++;
        }

    }
}
