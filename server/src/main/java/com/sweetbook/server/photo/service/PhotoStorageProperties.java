package com.sweetbook.server.photo.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.photo")
public record PhotoStorageProperties(
        String storageDir,
        long maxFileSizeBytes
) {

    public PhotoStorageProperties {
        if (storageDir == null || storageDir.isBlank()) {
            throw new IllegalArgumentException("app.photo.storage-dir 는 null 또는 공백일 수 없습니다.");
        }
        if (maxFileSizeBytes <= 0) {
            throw new IllegalArgumentException("app.photo.max-file-size-bytes 는 0보다 커야 합니다.");
        }
    }
}

