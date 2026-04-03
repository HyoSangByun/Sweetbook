package com.sweetbook.server.photo.service;

import com.sweetbook.server.album.domain.AlbumActivity;
import com.sweetbook.server.album.domain.AlbumProject;
import com.sweetbook.server.album.repository.AlbumActivityRepository;
import com.sweetbook.server.album.repository.AlbumProjectRepository;
import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
import com.sweetbook.server.photo.domain.ActivityPhoto;
import com.sweetbook.server.photo.dto.ActivityPhotoDeleteResponse;
import com.sweetbook.server.photo.dto.ActivityPhotoUploadResponse;
import com.sweetbook.server.photo.repository.ActivityPhotoRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ActivityPhotoService {

    private final AlbumProjectRepository albumProjectRepository;
    private final AlbumActivityRepository albumActivityRepository;
    private final ActivityPhotoRepository activityPhotoRepository;
    private final PhotoStorageProperties photoStorageProperties;

    @Transactional
    public ActivityPhotoUploadResponse uploadPhoto(Long userId, Long albumId, Long activityId, MultipartFile file) {
        validateImageFile(file);

        AlbumProject albumProject = getOwnedAlbum(userId, albumId);
        AlbumActivity albumActivity = albumActivityRepository
                .findByAlbumProjectIdAndActivityId(albumProject.getId(), activityId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ALBUM_ACTIVITY_NOT_FOUND,
                        "albumId=" + albumId + ", activityId=" + activityId
                ));

        String originalFileName = sanitizeFileName(file.getOriginalFilename());
        String extension = extractExtension(originalFileName);
        String storedFileName = UUID.randomUUID() + extension;

        Path storagePath = buildStoragePath(albumProject.getId(), activityId, storedFileName);
        try {
            Files.createDirectories(storagePath.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, storagePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "파일 저장 중 오류가 발생했습니다.");
        }

        ActivityPhoto activityPhoto = ActivityPhoto.builder()
                .albumActivity(albumActivity)
                .originalFileName(originalFileName)
                .storedFileName(storedFileName)
                .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                .fileSize(file.getSize())
                .storagePath(storagePath.toString())
                .build();
        ActivityPhoto saved = activityPhotoRepository.save(activityPhoto);

        return new ActivityPhotoUploadResponse(
                saved.getId(),
                saved.getOriginalFileName(),
                saved.getContentType(),
                saved.getFileSize(),
                saved.getStoragePath()
        );
    }

    @Transactional
    public ActivityPhotoDeleteResponse deletePhoto(Long userId, Long albumId, Long activityId, Long photoId) {
        AlbumProject albumProject = getOwnedAlbum(userId, albumId);
        AlbumActivity albumActivity = albumActivityRepository
                .findByAlbumProjectIdAndActivityId(albumProject.getId(), activityId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ALBUM_ACTIVITY_NOT_FOUND,
                        "albumId=" + albumId + ", activityId=" + activityId
                ));

        ActivityPhoto activityPhoto = activityPhotoRepository.findByIdAndAlbumActivityId(photoId, albumActivity.getId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PHOTO_NOT_FOUND,
                        "photoId=" + photoId
                ));

        Path filePath = Path.of(activityPhoto.getStoragePath());
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "파일 삭제 중 오류가 발생했습니다.");
        }

        activityPhotoRepository.delete(activityPhoto);
        return new ActivityPhotoDeleteResponse(true);
    }

    public boolean hasAnyPhotoInAlbum(Long albumId) {
        return activityPhotoRepository.existsByAlbumActivityAlbumProjectId(albumId);
    }

    private AlbumProject getOwnedAlbum(Long userId, Long albumId) {
        return albumProjectRepository.findByIdAndUserId(albumId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALBUM_NOT_FOUND));
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미지 파일이 비어 있습니다.");
        }
        if (file.getSize() > photoStorageProperties.maxFileSizeBytes()) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "이미지 파일 크기는 " + photoStorageProperties.maxFileSizeBytes() + " 바이트 이하여야 합니다."
            );
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미지 파일만 업로드할 수 있습니다.");
        }
    }

    private Path buildStoragePath(Long albumId, Long activityId, String storedFileName) {
        return Path.of(photoStorageProperties.storageDir())
                .resolve("albums")
                .resolve(String.valueOf(albumId))
                .resolve("activities")
                .resolve(String.valueOf(activityId))
                .resolve(storedFileName);
    }

    private String sanitizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "unknown";
        }
        return originalFileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String extractExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index);
    }
}

