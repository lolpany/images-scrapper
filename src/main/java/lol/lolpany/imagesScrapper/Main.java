package lol.lolpany.imagesScrapper;

import com.google.code.magja.model.product.ProductMedia;
import com.google.code.magja.service.ServiceException;
import org.apache.commons.lang3.tuple.Pair;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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


    public static void main(String[] args) throws CmdLineException, SQLException, ServiceException, IOException, GeneralSecurityException {
        disableSslChecks();
        new Main().doMain(args);


//        Set<String> attrs = new HashSet<>();
//        attrs.add("upc");
//        attrs.add("name");

//        com.google.code.magja.model.product.Product existringProduct = productService.getById(3351);
//        com.google.code.magja.model.product.Product product = productService.getById(3351);
//
//        ProductMedia productMedia = new ProductMedia();
//        Media media = new Media();
//        media.setData(FileUtils.readFileToByteArray(new File("D:\\buffer\\for-iPhone-6-6S-Plus-55.jpg")));
//        media.setName("for-iPhone-6-6S-Plus-55.jpg");
//        media.setMime("image/jpeg");
//        productMedia.setImage(media);
//        productMedia.setFile("for-iPhone-6-6S-Plus-55.jpg");
//        productMedia.setTypes(singleton(IMAGE));
//        product.setMedias(new ArrayList<>());
//        product.addMedia(productMedia);
//        productService.update(product, existringProduct);

//        System.out.println(productService.getById(3351).get
//        System.out.println(productService.getById(3351).getMedias().size());
//        System.out.println(productService.getById(3351).getMedias().get(0).getUrl());


//        Retrofit retrofit = new Retrofit.Builder()
//                .baseUrl("https://megacomponent.com/api/rest/")
//                .addConverterFactory(GsonConverterFactory.create())
//                .build();
//        MagentoApi magentoApi = retrofit.create(MagentoApi.class);
//        OAuthParameters oAuthParameters = new OAuthParameters();
//        oAuthParameters.consumerKey = "9764263afea2ba81a1d7f44bc608d491";
//        oAuthParameters.computeNonce();
//        oAuthParameters.signer = new OAuthHmacSigner();
//        oAuthParameters.signatureMethod = "HMAC-SHA1";
//        oAuthParameters.computeSignature(oAuthParameters.signatureMethod,
//                new GenericUrl("https://megacomponent.com/api/rest/"));
//        oAuthParameters.computeTimestamp();
//        oAuthParameters.version = "1.0a";
//        System.out.println(magentoApi.initiate(oAuthParameters.getAuthorizationHeader()).execute().toString());

//        System.out.println(magentoApi.readProduct(3351L).execute().toString());
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

        BlockingQueue<Product2> queue = new ArrayBlockingQueue<>(20);
        BlockingQueue<Product2> nextQueue = new ArrayBlockingQueue<>(20);
        // ~40kb * 20 = 40mb of memory
        BlockingQueue<Pair<String, byte[]>> fileQueue = new ArrayBlockingQueue<Pair<String, byte[]>>(10);

        int numberOfRunners = 1 + downloaders + writers + 3;

        ExecutorService executorService = new ThreadPoolExecutor(numberOfRunners,
                numberOfRunners, 10, TimeUnit.MINUTES,
                new ArrayBlockingQueue<Runnable>(numberOfRunners));

//        RemoteServiceFactory remoteServiceFactory = new RemoteServiceFactory(new MagentoSoapClient(
//                new SoapConfig("go", "golovac", "https://megacomponent.com/api/soap")
//        ));


//        ProductRemoteService productService = remoteServiceFactory.getProductRemoteService();
//        executorService.execute(new SoapApiProductReader(productService, from, n, queue));
//        executorService.execute(new ProductReader( DriverManager.getConnection(jdbcUrl, username, password),from, n, queue,
//                url, username, password, downloaders));
        executorService.execute(new CsvProductReader(null,from, n, queue,
                url, username, password, downloaders));


//        executorService.execute(new ProductReader(null,from, n, queue, url, username, password));
//        executorService.execute(new PicClickImageDownloader(productService, queue, nextQueue));
//        executorService.execute(new PicClickImageDownloader(productService, queue, nextQueue));
//        executorService.execute(new PicClickImageDownloader(productService, queue, nextQueue));
//        executorService.execute(new PicClickImageDownl|oader(productService, queue, nextQueue));


        List<BlockingQueue<StringBuilder>> csvWriterQueues = new ArrayList<>();
        for (int i = 0; i < downloaders; i++) {
            BlockingQueue<StringBuilder> csvWriterQueue = new ArrayBlockingQueue<>(5);
            executorService.execute(new EbayImageDownloader(null, queue,
                    fileQueue, imagesRoot, magmiDir, i, dumpEvery, csvWriterQueue));
            csvWriterQueues.add(csvWriterQueue);
        }

        executorService.execute(new CsvWriter(csvWriterQueues, csvWriterQueues.size()));

//        executorService.execute(new GoogleImageDownloader(null, nextQueue, n, fileQueue, imagesRoot, magmiDir, 1));


        for (int i = 0; i < writers; i++) {
            int endFiles ;
            if (i < writers) {
                endFiles = downloaders/writers;
            } else{
                endFiles = downloaders/writers + downloaders % writers;
            }
            executorService.execute(new ImageWriter(fileQueue, imageLocation, endFiles ));
        }

//        try {
//            long i = 1L;
//            queue.put(new Product(i++, imagePath, "68888758345", "68888758345"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "6629190801798", "6629190801798"));
//            queue.put(new Product(i++, imagePath, "857400005032", "857400005032"));
//            queue.put(new Product(i++, imagePath, "51128831717", "51128831717"));
//            queue.put(new Product(i++, imagePath, "867995000060", "867995000060"));
//            queue.put(new Product(i++, imagePath, "813085020371", "M&S Screen Protector Metallic Gold - iPhone COLORED FOR IPHONE 5/5S 33MM"));
//            queue.put(new Product(i++, imagePath, "841329100343", "iSHIELDZ Screen Protector - For 4.7\"iPhone AUTO ALIGN FOR IPHONE 6/6S"));
//            queue.put(new Product(i++, imagePath, "757120370932", ""));
//            queue.put(new Product(i++, imagePath, "\  ", ""));
//            queue.put(new Product(i++, imagePath, "37332195555", ""));
//            queue.put(new Product(i++, imagePath, "898745008692", ""));
//            queue.put(new Product(i++, imagePath, "884645105197", ""));
//            queue.put(new Product(i++, imagePath, "757120038603", ""));
//            queue.put(new Product(i++, imagePath, "37332170705", ""));
//            queue.put(new Product(i++, imagePath, "757120037866", ""));
//            queue.put(new Product(i++, imagePath, "65030811927", ""));
//            queue.put(new Product(i++, imagePath, "757120352112", ""));
//            queue.put(new Product(i++, imagePath, "UPC672792403422", "32-Port 3-User KVM over IP Switch - Dual Power/LAN"));
//            queue.put(new Product(i++, imagePath, "1123", "Black Box CAT5e Value Line Keystone Jack, Red, 25-Pack - 25 Pack - 1 x RJ-45 Female - Gold-plated Contacts - Red VALUE LINE"));
//            queue.put(new Product(i++, "d", "757120370369", "C2G 3.5mm 3-Conductor Keystone Adapter - Mini-phone - White CONDUCTOR"));
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
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

//    public static void setProductImage(ProductRemoteService productService, Product product, Product existingProduct,
//                                       String imageExtension, byte[] image) throws ServiceException, NoSuchAlgorithmException {
//        for(ProductMedia.Type type: IMAGE_TYPES) {
//            ProductMedia productMedia = new ProductMedia();
//            Media media = new Media();
//            media.setData(image);
//            media.setName(product.getId() + "." + imageExtension);
//            if (imageExtension.equals("jpg") || imageExtension.equals("jpeg")) {
//                media.setMime("image/jpeg");
//            } else {
//                media.setMime("image/" + imageExtension);
//            }
//            productMedia.setImage(media);
//            productMedia.setFile(product.getId() + "." + imageExtension);
//            productMedia.setTypes(singleton(type));
//            product.setMedias(new ArrayList<>());
//            product.addMedia(productMedia);
////            System.out.println("---------------------");
////            System.out.println(product.getId());
////            System.out.println(product.getAttributes().get("upc"));
////            System.out.println(product.getAttributes().get("name"));
//            productService.update(product, existingProduct);
////            System.out.println("done");
//        }
//    }
}
