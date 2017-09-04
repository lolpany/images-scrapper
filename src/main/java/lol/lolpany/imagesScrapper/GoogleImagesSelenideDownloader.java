package lol.lolpany.imagesScrapper;

import com.codeborne.selenide.Configuration;
import com.google.code.magja.service.product.ProductRemoteService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.codeborne.selenide.Selenide.switchTo;
import static java.lang.Thread.sleep;
import static lol.lolpany.imagesScrapper.ImageWriter.END_FILE;
import static lol.lolpany.imagesScrapper.Main.skuToFileName;
import static lol.lolpany.imagesScrapper.Product2.END_QUEUE;
import static lol.lolpany.imagesScrapper.Utils.identifyImageExtension;
import static org.apache.commons.io.IOUtils.toByteArray;

public class GoogleImagesSelenideDownloader implements Runnable {
    private static final Pattern META_DIV_URL_PATTERN = Pattern.compile(".*\"ou\":\"(.+?)[?\"].*");

    final ProductRemoteService productService;
    final BlockingQueue<Product2> inputQueue;
    final BlockingQueue<Pair<String, byte[]>> fileQueue;
    final String imagesRoot;
    final String magmiDir;
    final int downloaderIndex;
    final int dumpEvery;
    final BlockingQueue<StringBuilder> csvWriterQueue;


    GoogleImagesSelenideDownloader(ProductRemoteService productService, BlockingQueue<Product2> inputQueue,
                                   BlockingQueue<Pair<String, byte[]>> fileQueue, String imagesRoot,
                                   String magmiDir, int downloaderIndex, int dumpEvery,
                                   BlockingQueue<StringBuilder> csvWriterQueue) {
        this.productService = productService;
        this.inputQueue = inputQueue;
        this.fileQueue = fileQueue;
        this.imagesRoot = imagesRoot;
        this.magmiDir = magmiDir;
        this.downloaderIndex = downloaderIndex;
        this.dumpEvery = dumpEvery;
        this.csvWriterQueue = csvWriterQueue;
    }

    @Override
    public void run() {

        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("headless");
        WebDriver driver = new ChromeDriver(chromeOptions);



//        PhantomJSDriver driver = new PhantomJSDriver();



//        DesiredCapabilities capabilities = DesiredCapabilities.firefox();
//        capabilities.setCapability("marionette", true);
//        FirefoxProfile profile = new FirefoxProfile();
//        FirefoxOptions firefoxOptions = new FirefoxOptions();
//        firefoxOptions.setProfile(profile);
//        firefoxOptions.addCapabilities(capabilities);
//
//        FirefoxDriver driver = new FirefoxDriver(firefoxOptions);

//        capabilities.setCapability(org.openqa.selenium.remote.CapabilityType.PROXY, p);



        Configuration.baseUrl = "https://www.google.com/";

        StringBuilder result = new StringBuilder("");
        int i = 0;
        while (true) {
            try {
                Product2 productToDump = inputQueue.take();

                if (productToDump == END_QUEUE) {
                    break;
                }
                if (productToDump.name.contains("SW FOUND")) {
                    continue;
                }
                driver.get("https://www.google.ru/search?newwindow=1&tbm=isch&sa=1&q="
                        + URLEncoder.encode(productToDump.name, "UTF-8"));
                try {
                    driver.findElement(By.id("recaptcha"));
//                    switchTo().frame(0);
//                    driver.findElement(By.cssSelector("recaptcha-checkbox-checkmark")).click();
                    sleep(10000);
                    driver.findElement(By.name("submit")).submit();
                } catch (NoSuchElementException e) {
                    // do nothing
                }

                List<WebElement> metaDivs = driver.findElements(By.cssSelector("#isr_mc img.rg_ic"));
                if (metaDivs.size() > 12) {
                    metaDivs = metaDivs.subList(0, 12);
                }

                WebElement image = metaDivs.get(findLargestImageIndex(driver, 12));
                if (!"NULL".equals(productToDump.name) && image != null) {
                    image.click();
                    sleep(1000);
                    String imageSrc = driver.findElements(By.cssSelector("div#_YTc img.irc_mi")).get(1).getAttribute("src");
                    URL url = new URL(imageSrc);
                    URLConnection con = url.openConnection();
                    con.setConnectTimeout(100000);
                    con.setReadTimeout(100000);
                    byte[] imageBytes = toByteArray(con.getInputStream());
                    String imageExtension = identifyImageExtension(imageBytes);
                    if (imageExtension != null) {
                        String sku = productToDump.sku;

                        String fileName = skuToFileName(sku) + "." + imageExtension;

                        result.append("\n" + sku + "," + fileName + ","
                                + fileName
                                + "," + fileName);
                        i++;
                        if (i == dumpEvery) {
                            i = 0;
                            csvWriterQueue.put(result);
                            result = new StringBuilder("");
                        }
                        fileQueue.put(new ImmutablePair<>(imagesRoot + "\\" + fileName,
                                imageBytes));
                        sleep(2000 + Math.round( Math.random()) * 10000);
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
//            FileUtils.writeStringToFile(new File(magmiDir + File.separator +
//                    "google" + downloaderIndex + ".csv"), result.toString(), StandardCharsets.UTF_8);

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

    private int findLargestImageIndex(WebDriver driver, int limit) {
        int maxImageIndex = 0;
        int maxImageSize = 0;
        List<WebElement> elements = driver.findElements(By.cssSelector("span.rg_ilmn"));
        if (elements.size() > limit) {
            elements = elements.subList(0, limit);
        }
        int i = 0;
        for (WebElement element : elements) {
            String[] contentParts = element.getAttribute("textContent").split(" ");
            String[] dimensions = contentParts[1].split("×");
            int size = Integer.parseInt(dimensions[0].substring(0, dimensions[0].length() - 1))
                    * Integer.parseInt(dimensions[1].substring(1));
            if (size > maxImageSize) {
                maxImageSize = size;
                maxImageIndex = i;
            }
            i++;
        }
        return maxImageIndex;
    }
}
