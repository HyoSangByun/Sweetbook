package com.sweetbook.server.sweetbook.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sweetbook")
public record SweetbookProperties(
        String baseUrl,
        String apiKey,
        String bookSpecUid
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
    }
}

