package lol.lolpany.imagesScrapper;

import com.google.code.magja.model.product.ProductMedia;
import com.google.code.magja.service.ServiceException;
import org.apache.commons.lang3.tuple.Pair;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.phantomjs.PhantomJSDriver;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

import static com.google.code.magja.model.product.ProductMedia.Type.IMAGE;
import static com.google.code.magja.model.product.ProductMedia.Type.SMALL_IMAGE;

public class Main {

    final static int SIZE_LIMIT_TO_FILTER_OUT_BAD_AND_MANUFACTURER = 10 * 1024;
    final static int STARTING_IMAGE_SIZE = 130 * 1024;
    final static int MIN_IMAGE_SIZE = 10 * 1024;
    final static int IMAGE_SIZE_STEP = 10 * 1024;

    @Option(name = "-jdbcurl", usage = "jdbc url to connect to db")
    private String jdbcUrl;

    @Option(name = "-user", usage = "jdbc user")
    private String user;

    @Option(name = "-password", usage = "jdbc password")
    private String password;

    @Option(name = "-from", usage = "id of first product to scrap image for")
    private int from;

    @Option(name = "-n", usage = "number of products to scrap images for")
    private int number;

    @Option(name = "-imageslocation", usage = "root directory of magento images")
    private String imageLocation;

    @Option(name = "-magmidir", usage = "root directory of magento images")
    private String magmiDir;


    @Option(name = "-downloaders", usage = "number of downloaders")
    private int downloaders;

    @Option(name = "-writers", usage = "number of writers")
    private int writers;

    @Option(name = "-dumpthreshold", usage = "dump csv every")
    private int dumpEvery;


    private static final Set<ProductMedia.Type> IMAGE_TYPES = new HashSet<ProductMedia.Type>() {{
        add(IMAGE);
        add(SMALL_IMAGE);
//        add(THUMBNAIL);
    }};


    public static void main(String[] args) throws CmdLineException, SQLException, ServiceException,
            IOException, GeneralSecurityException {
        disableSslChecks();
        new Main().doMain(args);
    }

    public void doMain(String[] args) throws CmdLineException, SQLException {

        CmdLineParser parser = new CmdLineParser(this);
        parser.parseArgument(args);

        scrap(from, number, imageLocation, magmiDir, jdbcUrl, user, password, downloaders, writers,
                dumpEvery);

    }

    private void scrap(int from, int n, String imagesRoot, String magmiDir, String url,
                       String username, String password, int downloaders, int writers,
                       int dumpEvery) throws SQLException {

        BlockingQueue<Product2> queue = new ArrayBlockingQueue<>(downloaders *2);
        // ~40kb * 20 = 40mb of memory
        BlockingQueue<Pair<String, byte[]>> fileQueue = new ArrayBlockingQueue<>(downloaders *2);

        int numberOfRunners = 1 + downloaders + writers + 3;

        ExecutorService executorService = new ThreadPoolExecutor(numberOfRunners,
                numberOfRunners, 10, TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(numberOfRunners));

        executorService.execute(new CsvProductReader(null,from, n, queue,
                url, username, password, downloaders));

//        System.setProperty("webdriver.gecko.driver", "D:\\buffer\\geckodriver-v0.17.0-win64\\geckodriver.exe");
        System.setProperty("webdriver.chrome.driver", "D:\\buffer\\chromedriver\\chromedriver.exe");


        List<BlockingQueue<StringBuilder>> csvWriterQueues = new ArrayList<>();
        for (int i = 0; i < downloaders; i++) {
            BlockingQueue<StringBuilder> csvWriterQueue = new ArrayBlockingQueue<>(5);
            executorService.execute(
//                    new GoogleImagesSelenideDownloader(null, queue, fileQueue, imagesRoot, magmiDir, i, dumpEvery,
//                            csvWriterQueue));
            new EbayImageDownloader(null, queue, fileQueue, imagesRoot, magmiDir, i, dumpEvery,
                    csvWriterQueue));
            csvWriterQueues.add(csvWriterQueue);
        }

        executorService.execute(new CsvWriter(csvWriterQueues, csvWriterQueues.size()));

        for (int i = 0; i < writers; i++) {
            int endFiles ;
            if (i < writers) {
                endFiles = downloaders/writers;
            } else {
                endFiles = downloaders/writers + downloaders % writers;
            }
            executorService.execute(new ImageWriter(fileQueue, imageLocation, endFiles ));
        }
    }

    private static void disableSslChecks() {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
        }
    }

}
