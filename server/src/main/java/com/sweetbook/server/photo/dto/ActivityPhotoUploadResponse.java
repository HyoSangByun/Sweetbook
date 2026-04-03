package com.sweetbook.server.photo.dto;

public record ActivityPhotoUploadResponse(
        Long photoId,
        String originalFileName,
        String contentType,
        long fileSize
) {
}
