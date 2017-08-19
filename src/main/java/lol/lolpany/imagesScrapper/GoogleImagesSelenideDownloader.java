package lol.lolpany.imagesScrapper;

import java.util.List;

public class GoogleImagesSelenideDownloader  implements Runnable  {

    @Override
    public void run() {
        System.setProperty("webdriver.gecko.driver", "D:\\buffer\\geckodriver-v0.17.0-win64\\geckodriver.exe");

        DesiredCapabilities capabilities = DesiredCapabilities.firefox();
        capabilities.setCapability("marionette", true);
//        capabilities.setCapability(org.openqa.selenium.remote.CapabilityType.PROXY, p);

        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("network.proxy.type", 1);
        profile.setPreference("network.proxy.socks", "127.0.0.1");
        profile.setPreference("network.proxy.socks_port", 9050);


        FirefoxOptions firefoxOptions = new FirefoxOptions();
        firefoxOptions.setProfile(profile);
        firefoxOptions.addCapabilities(capabilities);

        WebDriver driver = new FirefoxDriver(firefoxOptions);
        Configuration.baseUrl = "https://www.linkedin.com/";

        if (!login(driver)) {
            throw new Exception();
        }
        sleep(20000);

        for (int i = startPage; i < endPage; i++) {
            System.out.println(i);
            driver.get("https://www.linkedin.com/search/results/people/?facetGeoRegion=%5B%22us%3A0%22%5D&facetNetwork=%5B%22S%22%5D&origin=FACETED_SEARCH&page=" + i);
            JavascriptExecutor jse = (JavascriptExecutor) driver;

//
//            new WebDriverWait(driver, 10).until((ExpectedCondition<Boolean>) wd ->
//                    ((JavascriptExecutor) wd).executeScript("return document.readyState").equals("complete"));

            sleep(15000);

            jse.executeScript("window.scrollBy(0,500)", "");

            sleep(5000);


            List<WebElement> buttons = driver.findElements(By.className("search-result__actions--primary"));


            for (int j = 0; j < buttons.size(); j++) {
                jse.executeScript("window.scrollBy(0,1000)", "");
                WebElement button = driver.findElements(By.className("search-result__actions--primary")).get(j);
                if (button != null) {
                    System.out.println(button.getText());
                    if ("Connect".equals(button.getText())) {
                        try {
                            button.click();
                            WebElement connectDiaglog = null;
                            while (connectDiaglog == null) {
                                connectDiaglog = driver.findElement(By.className("modal-wormhole-content"));
                            }

                            WebElement sendButton = null;
                            while (sendButton == null) {
                                sendButton = connectDiaglog.findElement(By.className("send-invite__actions"))
                                        .findElement(By.className("button-primary-large"))
                                ;
                            }
                            sendButton.submit();
                            sleep(5000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        }
    }
}
