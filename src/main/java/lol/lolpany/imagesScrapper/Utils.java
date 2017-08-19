package lol.lolpany.imagesScrapper;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class Utils {
    @Test
    public void diff() throws IOException {
        FileUtils.writeLines(new File("D:\\buffer\\scrapper\\univold-notdowned.csv"),
                diff("D:\\buffer\\scrapper\\out.csv", "D:\\buffer\\downedcsv\\univold0.csv"));
    }

    public Collection<String> diff(String toDownload, String downloaded) throws FileNotFoundException {
        Map<String, String> toDownloadMap = new HashMap<>();
        Scanner toDownloadScanner = new Scanner(new File(toDownload));
        toDownloadScanner.nextLine();
        while (toDownloadScanner.hasNext()) {
            String line = toDownloadScanner.nextLine();
            String[] lineParts = line.split("\t");
            if (lineParts.length > 1) {
                toDownloadMap.put(lineParts[1], line);
            }
        }

        Set<String> downloadedSet = new HashSet<>();
        Scanner downloadedScanner = new Scanner(new File(downloaded));
        downloadedScanner.nextLine();
        downloadedScanner.nextLine();
        while (downloadedScanner.hasNext()) {
            String[] lineParts = downloadedScanner.nextLine().split(",");
            if (lineParts.length > 1) {
            downloadedSet.add(lineParts[0]);
            }
        }

        for (String sku: downloadedSet) {
            toDownloadMap.remove(sku);
        }
        return toDownloadMap.values();
    }
}
