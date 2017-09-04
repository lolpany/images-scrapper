package lol.lolpany.imagesScrapper;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class Utils {

    public static String identifyImageExtension(byte[] imageBytes) throws IOException {
        ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes));

        Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(iis);

        if (imageReaders.hasNext()) {
            return imageReaders.next().getFormatName();
        }
        return null;
    }

    @Test
    public void diff() throws IOException {
        FileUtils.writeLines(new File("D:\\buffer\\scrapper\\mega-notdowned.csv"),
                diff("D:\\buffer\\scrapper\\out (1).csv", "D:\\buffer\\downedcsv\\mega-downed-second.csv"));
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

        for (String sku : downloadedSet) {
            toDownloadMap.remove(sku);
        }
        return toDownloadMap.values();
    }


    @Test
    public void renameFiles() throws IOException {
        Files.walkFileTree(Paths.get("D:\\buffer\\magmi2"), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                File current = file.toFile();
                if (current.isFile()) {
                    String fileName = current.getName();
                    String[] fileNameParts = fileName.split("\\.");
                    current.renameTo(new File("D:\\buffer\\magmiend" + File.separator + fileNameParts[0]
                            + fileNameParts[fileNameParts.length - 1]));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e)
                    throws IOException {
                if (e == null) {
                    return FileVisitResult.CONTINUE;
                } else {
                    // directory iteration failed
                    throw e;
                }
            }
        });
    }

    @Test
    public void goRescrapZeroSize() throws IOException {
        FileUtils.writeLines(new File("D:\\buffer\\scrapper\\univold-zerosize-todown.csv"),
                rescrapZeroSize("D:\\buffer\\scrapper\\univold-zerosize.csv", "D:\\buffer\\scrapper\\out (1).csv"));
    }

    public Collection<String> rescrapZeroSize(String zeroSize, String all) throws FileNotFoundException {
        Map<String, String> toDownloadMap = new HashMap<>();
        Scanner toDownloadScanner = new Scanner(new File(all));
        toDownloadScanner.nextLine();
        while (toDownloadScanner.hasNext()) {
            String line = toDownloadScanner.nextLine();
            String[] lineParts = line.split("\t");
            if (lineParts.length > 1) {
                toDownloadMap.put(lineParts[1], line);
            }
        }

        Set<String> downloadedSet = new HashSet<>();
        Scanner downloadedScanner = new Scanner(new File(zeroSize));
        downloadedScanner.nextLine();
        downloadedScanner.nextLine();
        while (downloadedScanner.hasNext()) {
            String[] lineParts = downloadedScanner.nextLine().split("/");
            if (lineParts.length > 1) {
                String fileName = lineParts[lineParts.length - 1];
                downloadedSet.add(fileName.substring(0, fileName.lastIndexOf(".")).toUpperCase());
            }
        }

        List<String> result = new ArrayList<>();
        for (String sku : downloadedSet) {
            String product = toDownloadMap.get(sku);
            if (product == null) {
                product = toDownloadMap.get(" " + sku);
            }
            if (product != null) {
                result.add(product);
            }
        }
        return result;
    }

    @Test
    public void goImage() throws IOException {
        FileUtils.writeLines(new File("D:\\buffer\\scrapper\\univold-one-image.csv"),
                oneImageForAll("D:\\buffer\\scrapper\\out.csv", "asdf.png", "SW FOUND"));
    }

    public Collection<String> oneImageForAll(String toImportFiles, String imageName, String productName) throws FileNotFoundException {
        List<String> result = new ArrayList<>();
        result.add("sku,image,small_image,thumbnail");
        Scanner toDownloadScanner = new Scanner(new File(toImportFiles));
        toDownloadScanner.nextLine();
        while (toDownloadScanner.hasNext()) {
            String line = toDownloadScanner.nextLine();
            if (line.contains(productName)) {
                String[] lineParts = line.split("\t");
                if (lineParts.length > 1) {
                    result.add(lineParts[1] + "," + imageName + "," + imageName + "," + imageName);
                }
            }
        }
        return result;
    }

    @Test
    public void imagesNotPresent() throws IOException {
        FileUtils.writeLines(new File("D:\\buffer\\scrapper\\mega-small.csv"),
                imagesNotPresent("D:\\buffer\\scrapper\\getallprods-mega.csv", "D:\\buffer\\scrapper\\mega-small-images.csv"));
    }

    public Collection<String> imagesNotPresent(String fromDb, String fromFs) throws FileNotFoundException {
        Set<String> onFs = new HashSet<>();
        Scanner toDownloadScanner = new Scanner(new File(fromFs));
        while (toDownloadScanner.hasNext()) {
            String line = toDownloadScanner.nextLine();
            String[] lineParts = line.split("/", 2);
            if (lineParts.length == 2) {
                onFs.add("/" + lineParts[1]);
            }
        }

        List<String> result = new ArrayList<>();
        result.add("id,sku,name,image");
        Scanner fromDbScann = new Scanner(new File(fromDb));
        fromDbScann.nextLine();
        while (fromDbScann.hasNext()) {
            String line = fromDbScann.nextLine();
            String[] lineParts = line.split("\t");
            if (lineParts.length == 4) {
                if (onFs.contains(lineParts[3])) {
                    result.add(line);
                }
            }
        }
        return result;
    }
}
