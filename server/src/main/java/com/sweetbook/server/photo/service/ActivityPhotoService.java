package com.sweetbook.server.photo.service;

import com.sweetbook.server.album.domain.AlbumActivity;
import com.sweetbook.server.album.domain.AlbumProject;
import com.sweetbook.server.album.repository.AlbumActivityRepository;
import com.sweetbook.server.album.repository.AlbumProjectRepository;
import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
import com.sweetbook.server.photo.domain.ActivityPhoto;
import com.sweetbook.server.photo.dto.ActivityPhotoDeleteResponse;
import com.sweetbook.server.photo.dto.ActivityPhotoItemResponse;
import com.sweetbook.server.photo.dto.ActivityPhotoUploadResponse;
import com.sweetbook.server.photo.repository.ActivityPhotoRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ActivityPhotoService {

    private static final Logger log = LoggerFactory.getLogger(ActivityPhotoService.class);

    private final AlbumProjectRepository albumProjectRepository;
    private final AlbumActivityRepository albumActivityRepository;
    private final ActivityPhotoRepository activityPhotoRepository;
    private final PhotoStorageProperties photoStorageProperties;

    @Transactional
    public ActivityPhotoUploadResponse uploadPhoto(Long albumId, Long activityId, MultipartFile file) {
        validateImageFile(file);

        AlbumProject albumProject = getOwnedAlbum(albumId);
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

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Failed to read file bytes.");
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

        registerAfterCommitFileCopy(storagePath, fileBytes);

        return new ActivityPhotoUploadResponse(
                saved.getId(),
                saved.getOriginalFileName(),
                saved.getContentType(),
                saved.getFileSize()
        );
    }

    @Transactional
    public ActivityPhotoDeleteResponse deletePhoto(Long albumId, Long activityId, Long photoId) {
        AlbumProject albumProject = getOwnedAlbum(albumId);
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
        activityPhotoRepository.delete(activityPhoto);
        registerAfterCommitFileDelete(filePath);

        return new ActivityPhotoDeleteResponse(true);
    }

    public boolean hasAnyPhotoInAlbum(Long albumId) {
        return activityPhotoRepository.existsByAlbumActivityAlbumProjectId(albumId);
    }

    @Transactional
    public void deleteAllForAlbumActivity(AlbumActivity albumActivity) {
        List<ActivityPhoto> photos = activityPhotoRepository.findAllByAlbumActivityIdOrderByCreatedAtDesc(albumActivity.getId());
        for (ActivityPhoto photo : photos) {
            registerAfterCommitFileDelete(Path.of(photo.getStoragePath()));
        }
        activityPhotoRepository.deleteAll(photos);
    }

    @Transactional(readOnly = true)
    public List<ActivityPhotoItemResponse> listPhotos(Long albumId, Long activityId) {
        AlbumProject albumProject = getOwnedAlbum(albumId);
        AlbumActivity albumActivity = albumActivityRepository
                .findByAlbumProjectIdAndActivityId(albumProject.getId(), activityId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ALBUM_ACTIVITY_NOT_FOUND,
                        "albumId=" + albumId + ", activityId=" + activityId
                ));

        return activityPhotoRepository.findAllByAlbumActivityIdOrderByCreatedAtDesc(albumActivity.getId()).stream()
                .map(photo -> new ActivityPhotoItemResponse(
                        photo.getId(),
                        photo.getOriginalFileName(),
                        photo.getContentType(),
                        photo.getFileSize(),
                        photo.getCreatedAt()
                ))
                .toList();
    }

    private AlbumProject getOwnedAlbum(Long albumId) {
        return albumProjectRepository.findById(albumId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALBUM_NOT_FOUND));
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Image file is empty.");
        }
        if (file.getSize() > photoStorageProperties.maxFileSizeBytes()) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Image file size exceeds max limit: " + photoStorageProperties.maxFileSizeBytes()
            );
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Only image files are allowed.");
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

    private void registerAfterCommitFileCopy(Path storagePath, byte[] fileBytes) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Transaction synchronization is not active.");
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    Files.createDirectories(storagePath.getParent());
                    Path tempPath = storagePath.resolveSibling(storagePath.getFileName() + ".tmp");
                    Files.write(tempPath, fileBytes);
                    Files.move(tempPath, storagePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException ex) {
                    log.error("Failed to store photo after commit. path={}", storagePath, ex);
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    Path tempPath = storagePath.resolveSibling(storagePath.getFileName() + ".tmp");
                    try {
                        Files.deleteIfExists(tempPath);
                    } catch (IOException ex) {
                        log.warn("Failed to cleanup temp file after rollback. path={}", tempPath, ex);
                    }
                }
            }
        });
    }

    private void registerAfterCommitFileDelete(Path filePath) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Transaction synchronization is not active.");
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    Files.deleteIfExists(filePath);
                } catch (IOException ex) {
                    log.error("Failed to delete photo after commit. path={}", filePath, ex);
                }
            }
        });
    }
}
