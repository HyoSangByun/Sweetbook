package com.sweetbook.server.sweetbook.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sweetbook")
public record SweetbookProperties(
        String baseUrl,
        String apiKey,
        String bookSpecUid,
        Duration connectTimeout,
        Duration readTimeout
) {

    public SweetbookProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("app.sweetbook.base-url is required.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("app.sweetbook.api-key is required.");
        }
        if (bookSpecUid == null || bookSpecUid.isBlank()) {
            throw new IllegalArgumentException("app.sweetbook.book-spec-uid is required.");
        }
        if (connectTimeout == null || connectTimeout.isZero() || connectTimeout.isNegative()) {
            throw new IllegalArgumentException("app.sweetbook.connect-timeout must be positive.");
        }
        if (readTimeout == null || readTimeout.isZero() || readTimeout.isNegative()) {
            throw new IllegalArgumentException("app.sweetbook.read-timeout must be positive.");
        }
    }
}
