package com.seolytics.crawler;

import com.seolytics.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

@Component
public class UrlSafetyValidator {

    public URI validatePublicHttpUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "URL is required");
        }
        String trimmed = raw.trim();
        URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "The URL is not valid");
        }
        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Enter a full URL including https://");
        }
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Only http and https URLs are allowed");
        }
        if (uri.getUserInfo() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "URLs with credentials are not allowed");
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (isBlockedHost(host)) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Local and private network URLs cannot be crawled");
        }
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (isBlockedAddress(address)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Local and private network URLs cannot be crawled");
                }
            }
        } catch (UnknownHostException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "The hostname could not be resolved");
        }
        return uri.normalize();
    }

    public boolean isSafeHttpUrl(String raw) {
        try {
            validatePublicHttpUrl(raw);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isBlockedHost(String host) {
        return host.equals("localhost")
                || host.endsWith(".localhost")
                || host.endsWith(".local")
                || host.equals("0.0.0.0")
                || host.equals("::1")
                || host.equals("[::1]");
    }

    private boolean isBlockedAddress(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress();
    }
}
