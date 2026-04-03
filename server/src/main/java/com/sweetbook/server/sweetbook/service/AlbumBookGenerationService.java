package com.sweetbook.server.sweetbook.service;

import com.sweetbook.server.activity.domain.Activity;
import com.sweetbook.server.album.domain.AlbumActivity;
import com.sweetbook.server.album.domain.AlbumProject;
import com.sweetbook.server.album.dto.GenerateBookResponse;
import com.sweetbook.server.album.repository.AlbumActivityRepository;
import com.sweetbook.server.album.repository.AlbumProjectRepository;
import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
import com.sweetbook.server.photo.domain.ActivityPhoto;
import com.sweetbook.server.photo.repository.ActivityPhotoRepository;
import com.sweetbook.server.sweetbook.client.SweetbookBooksClient;
import com.sweetbook.server.sweetbook.config.SweetbookProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlbumBookGenerationService {

    private static final String COVER_TEMPLATE_UID = "40nimglmWLSh";
    private static final String MONTH_START_TEMPLATE_UID = "7kV0VVvWlwNI";
    private static final String CONTENT_TEMPLATE_WITH_IMAGE = "1XtN1225R7wN";
    private static final String CONTENT_TEMPLATE_NO_IMAGE = "5ZpsyEJW5PZW";

    private final AlbumProjectRepository albumProjectRepository;
    private final AlbumActivityRepository albumActivityRepository;
    private final ActivityPhotoRepository activityPhotoRepository;
    private final SweetbookBooksClient sweetbookBooksClient;
    private final SweetbookProperties sweetbookProperties;

    @Transactional
    public GenerateBookResponse generateBook(Long userId, Long albumId) {
        AlbumProject albumProject = albumProjectRepository.findByIdAndUserId(albumId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALBUM_NOT_FOUND));

        List<AlbumActivity> albumActivities =
                albumActivityRepository.findAllByAlbumProjectIdOrderByActivityActivityDateTimeDesc(albumId);
        if (albumActivities.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "No selected activities in album.");
        }

        boolean hasPhoto = activityPhotoRepository.existsByAlbumActivityAlbumProjectId(albumId);
        String contentTemplateUid = hasPhoto ? CONTENT_TEMPLATE_WITH_IMAGE : CONTENT_TEMPLATE_NO_IMAGE;

        String externalRef = "album-" + albumProject.getId() + "-" + System.currentTimeMillis();
        String bookUid = sweetbookBooksClient.createBook(
                albumProject.getTitle(),
                sweetbookProperties.bookSpecUid(),
                externalRef
        );

        sweetbookBooksClient.addCover(bookUid, COVER_TEMPLATE_UID, buildCoverParameters(albumProject));
        sweetbookBooksClient.addContent(bookUid, MONTH_START_TEMPLATE_UID, buildMonthStartParameters(albumProject), "page");

        for (AlbumActivity albumActivity : albumActivities) {
            Map<String, Object> parameters = buildActivityContentParameters(albumActivity);
            if (hasPhoto) {
                resolveUploadedPhotoFileName(bookUid, albumActivity).ifPresent(fileName -> parameters.put("photo", fileName));
            }
            sweetbookBooksClient.addContent(bookUid, contentTemplateUid, parameters, "page");
        }

        sweetbookBooksClient.finalizeBook(bookUid);

        LocalDateTime generatedAt = LocalDateTime.now();
        albumProject.markBookGenerated(bookUid, generatedAt);

        return new GenerateBookResponse(
                albumProject.getId(),
                bookUid,
                albumProject.getBookStatus(),
                hasPhoto,
                COVER_TEMPLATE_UID,
                MONTH_START_TEMPLATE_UID,
                contentTemplateUid,
                albumActivities.size() + 1,
                generatedAt
        );
    }

    private Map<String, Object> buildCoverParameters(AlbumProject albumProject) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("title", albumProject.getTitle());
        parameters.put("subtitle", albumProject.getSubtitle());
        parameters.put("month", albumProject.getMonth());
        return parameters;
    }

    private Map<String, Object> buildMonthStartParameters(AlbumProject albumProject) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("month", albumProject.getMonth());
        parameters.put("monthlyReview", albumProject.getMonthlyReview());
        return parameters;
    }

    private Map<String, Object> buildActivityContentParameters(AlbumActivity albumActivity) {
        Activity activity = albumActivity.getActivity();
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("activityDateTime", activity.getActivityDateTime().toString());
        parameters.put("activityName", activity.getActivityName());
        parameters.put("activityType", activity.getActivityType());
        parameters.put("distanceKm", activity.getDistanceKm());
        parameters.put("movingTimeSeconds", activity.getMovingTimeSeconds());
        parameters.put("memo", albumActivity.getMemo());
        return parameters;
    }

    private java.util.Optional<String> resolveUploadedPhotoFileName(String bookUid, AlbumActivity albumActivity) {
        List<ActivityPhoto> photos = activityPhotoRepository.findAllByAlbumActivityIdOrderByCreatedAtDesc(albumActivity.getId());
        if (photos.isEmpty()) {
            return java.util.Optional.empty();
        }

        ActivityPhoto photo = photos.get(0);
        Path path = Path.of(photo.getStoragePath());
        if (!Files.exists(path)) {
            return java.util.Optional.empty();
        }

        String fileName = sweetbookBooksClient.uploadPhoto(bookUid, path);
        return java.util.Optional.of(fileName);
    }
}

