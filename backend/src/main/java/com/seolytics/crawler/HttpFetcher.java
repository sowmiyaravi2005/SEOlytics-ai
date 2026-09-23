package com.seolytics.crawler;

import com.seolytics.config.SeolyticsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class HttpFetcher {

    private static final Logger log = LoggerFactory.getLogger(HttpFetcher.class);

    private final SeolyticsProperties properties;
    private final UrlSafetyValidator urlSafetyValidator;
    private final HttpClient client;

    public HttpFetcher(SeolyticsProperties properties, UrlSafetyValidator urlSafetyValidator) {
        this.properties = properties;
        this.urlSafetyValidator = urlSafetyValidator;
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofMillis(properties.getCrawler().getRequestTimeoutMs()))
                .build();
    }

    public FetchResult fetch(String url, boolean includeBody) {
        long start = System.currentTimeMillis();
        List<Integer> hops = new ArrayList<>();
        String current = url;
        try {
            for (int i = 0; i <= properties.getCrawler().getMaxRedirectHops(); i++) {
                URI uri = urlSafetyValidator.validatePublicHttpUrl(current);
                HttpRequest request = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofMillis(properties.getCrawler().getRequestTimeoutMs()))
                        .header("User-Agent", properties.getCrawler().getUserAgent())
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .GET()
                        .build();
                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                int status = response.statusCode();
                hops.add(status);
                Optional<String> location = response.headers().firstValue("location");
                if (isRedirect(status) && location.isPresent() && i < properties.getCrawler().getMaxRedirectHops()) {
                    current = uri.resolve(location.get()).toString();
                    continue;
                }
                String contentType = response.headers().firstValue("content-type").orElse("");
                String body = "";
                if (includeBody && response.body() != null) {
                    int max = properties.getCrawler().getMaxHtmlBytes();
                    byte[] bytes = response.body();
                    if (bytes.length > max) {
                        body = new String(bytes, 0, max, StandardCharsets.UTF_8);
                    } else {
                        body = new String(bytes, StandardCharsets.UTF_8);
                    }
                }
                return new FetchResult(current, status, body, contentType, Math.max(0, hops.size() - 1),
                        hops, System.currentTimeMillis() - start, null);
            }
            return FetchResult.failure(url, "Redirect chain exceeded the configured hop limit", System.currentTimeMillis() - start);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return FetchResult.failure(url, "Request interrupted", System.currentTimeMillis() - start);
        } catch (Exception ex) {
            log.debug("Fetch failed for {}: {}", url, ex.getMessage());
            return FetchResult.failure(url, ex.getMessage(), System.currentTimeMillis() - start);
        }
    }

    public FetchResult headOrGetStatus(String url) {
        long start = System.currentTimeMillis();
        try {
            URI uri = urlSafetyValidator.validatePublicHttpUrl(url);
            HttpRequest head = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofMillis(Math.min(8000, properties.getCrawler().getRequestTimeoutMs())))
                    .header("User-Agent", properties.getCrawler().getUserAgent())
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<Void> response = client.send(head, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() == 405 || response.statusCode() == 501) {
                return fetch(url, false);
            }
            return new FetchResult(url, response.statusCode(), "", "", 0, List.of(response.statusCode()),
                    System.currentTimeMillis() - start, null);
        } catch (Exception ex) {
            return FetchResult.failure(url, ex.getMessage(), System.currentTimeMillis() - start);
        }
    }

    private boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }
}
