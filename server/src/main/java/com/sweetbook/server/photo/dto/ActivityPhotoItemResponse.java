package com.sweetbook.server.photo.dto;

import java.time.LocalDateTime;

public record ActivityPhotoItemResponse(
        Long photoId,
        String originalFileName,
        String contentType,
        long fileSize,
        LocalDateTime createdAt
) {
}

