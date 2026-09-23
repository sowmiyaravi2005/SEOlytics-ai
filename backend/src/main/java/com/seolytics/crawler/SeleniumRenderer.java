package com.seolytics.crawler;

import com.seolytics.config.SeolyticsProperties;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class SeleniumRenderer {

    private static final Logger log = LoggerFactory.getLogger(SeleniumRenderer.class);
    private final SeolyticsProperties properties;

    public SeleniumRenderer(SeolyticsProperties properties) {
        this.properties = properties;
    }

    public String render(String url) {
        if (!properties.getCrawler().isUseSelenium()) {
            return null;
        }
        RemoteWebDriver driver = null;
        try {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--window-size=1280,900");
            driver = new ChromeDriver(options);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofMillis(properties.getCrawler().getRequestTimeoutMs()));
            driver.get(url);
            return driver.getPageSource();
        } catch (Exception ex) {
            log.warn("Selenium rendering skipped for {}: {}", url, ex.getMessage());
            return null;
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception ignored) {
                    // ignore close errors
                }
            }
        }
    }
}
