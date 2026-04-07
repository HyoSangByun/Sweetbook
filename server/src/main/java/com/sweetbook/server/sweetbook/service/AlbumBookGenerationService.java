package com.sweetbook.server.sweetbook.service;

import com.sweetbook.server.activity.domain.Activity;
import com.sweetbook.server.album.domain.AlbumActivity;
import com.sweetbook.server.album.domain.AlbumProject;
import com.sweetbook.server.album.domain.BookGenerationStatus;
import com.sweetbook.server.album.dto.BookEstimateRequest;
import com.sweetbook.server.album.dto.BookEstimateResponse;
import com.sweetbook.server.album.dto.GenerateBookRequest;
import com.sweetbook.server.album.dto.GenerateBookResponse;
import com.sweetbook.server.album.repository.AlbumActivityRepository;
import com.sweetbook.server.album.repository.AlbumProjectRepository;
import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
import com.sweetbook.server.photo.domain.ActivityPhoto;
import com.sweetbook.server.photo.repository.ActivityPhotoRepository;
import com.sweetbook.server.sweetbook.client.SweetbookBooksClient;
import com.sweetbook.server.sweetbook.client.SweetbookOrdersClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class AlbumBookGenerationService {

    private static final String COVER_TEMPLATE_UID = "40nimglmWLSh";
    private static final String MONTH_START_TEMPLATE_UID = "7kV0VVvWlwNI";

    private final AlbumProjectRepository albumProjectRepository;
    private final AlbumActivityRepository albumActivityRepository;
    private final ActivityPhotoRepository activityPhotoRepository;
    private final SweetbookBooksClient sweetbookBooksClient;
    private final SweetbookOrdersClient sweetbookOrdersClient;
    private final PlatformTransactionManager transactionManager;

    public GenerateBookResponse generateBook(Long userId, Long albumId, GenerateBookRequest request) {
        GenerationPreparation preparation = prepareGeneration(userId, albumId);
        if (preparation.alreadyGenerated()) {
            return buildGeneratedResponse(preparation, request.coverTemplateUid(), request.contentTemplateUid());
        }

        validateTemplatesForBookSpec(
                request.bookSpecUid(),
                request.coverTemplateUid(),
                request.contentTemplateUid()
        );

        String bookUid = sweetbookBooksClient.createBook(
                request.title(),
                request.bookSpecUid(),
                preparation.externalRef()
        );

        sweetbookBooksClient.addCover(bookUid, request.coverTemplateUid(), buildCoverParameters(preparation, request.title()));
        sweetbookBooksClient.addContent(bookUid, MONTH_START_TEMPLATE_UID, buildMonthStartParameters(preparation), "page");

        for (ActivityPageData activityPage : preparation.activityPages()) {
            Map<String, Object> parameters = buildActivityContentParameters(activityPage);
            if (preparation.hasPhoto()) {
                resolveUploadedPhotoFileName(bookUid, activityPage.albumActivityId())
                        .ifPresent(fileName -> parameters.put("photo", fileName));
            }
            sweetbookBooksClient.addContent(bookUid, request.contentTemplateUid(), parameters, "page");
        }

        sweetbookBooksClient.finalizeBook(bookUid);

        return markGenerated(userId, albumId, preparation, request, bookUid);
    }

    @Transactional(readOnly = true)
    public BookEstimateResponse estimateOrder(Long userId, Long albumId, BookEstimateRequest request) {
        AlbumProject albumProject = albumProjectRepository.findByIdAndUserId(albumId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALBUM_NOT_FOUND));
        List<AlbumActivity> albumActivities =
                albumActivityRepository.findAllByAlbumProjectIdOrderByActivityActivityDateTimeDesc(albumId);
        if (albumActivities.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "No selected activities in album.");
        }

        List<ActivityPageData> activityPages = mapActivityPages(albumActivities);
        String externalRef = "preview-" + albumId + "-" + UUID.randomUUID();
        String bookUid = sweetbookBooksClient.createBook(request.title(), request.bookSpecUid(), externalRef);

        try {
            sweetbookBooksClient.addCover(
                    bookUid,
                    request.coverTemplateUid(),
                    buildEstimateCoverParameters(request.title(), albumProject)
            );

            for (ActivityPageData activityPage : activityPages) {
                sweetbookBooksClient.addContent(
                        bookUid,
                        request.contentTemplateUid(),
                        buildActivityContentParameters(activityPage),
                        "page"
                );
            }

            Map<String, Object> estimate = sweetbookOrdersClient.estimateOrder(bookUid, 1);
            return new BookEstimateResponse(
                    activityPages.size() + 1,
                    toLong(estimate.get("productAmount")),
                    toLong(estimate.get("shippingFee")),
                    toLong(estimate.get("packagingFee")),
                    toLong(estimate.get("totalAmount")),
                    estimate.get("currency") == null ? "KRW" : String.valueOf(estimate.get("currency"))
            );
        } finally {
            sweetbookBooksClient.deleteBook(bookUid);
        }
    }

    private GenerationPreparation prepareGeneration(Long userId, Long albumId) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        return template.execute(status -> {
            AlbumProject albumProject = albumProjectRepository.findByIdAndUserId(albumId, userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ALBUM_NOT_FOUND));

            List<AlbumActivity> albumActivities =
                    albumActivityRepository.findAllByAlbumProjectIdOrderByActivityActivityDateTimeDesc(albumId);
            if (albumActivities.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "No selected activities in album.");
            }

            boolean hasPhoto = activityPhotoRepository.existsByAlbumActivityAlbumProjectId(albumId);
            String externalRef = resolveExternalRef(albumProject);
            List<ActivityPageData> activityPages = mapActivityPages(albumActivities);

            if (albumProject.getBookUid() != null && !albumProject.getBookUid().isBlank()) {
                if (albumProject.getBookStatus() == BookGenerationStatus.GENERATED) {
                    if (albumProject.getBookExternalRef() == null || albumProject.getBookExternalRef().isBlank()) {
                        albumProject.markBookGenerationPending(externalRef);
                        albumProject.markBookGenerated(albumProject.getBookUid(), albumProject.getBookGeneratedAt());
                        albumProjectRepository.saveAndFlush(albumProject);
                    }
                    return new GenerationPreparation(
                            albumProject.getId(),
                            albumProject.getTitle(),
                            albumProject.getMonth(),
                            albumProject.getSubtitle(),
                            albumProject.getMonthlyReview(),
                            externalRef,
                            hasPhoto,
                            activityPages,
                            true,
                            albumProject.getBookUid(),
                            albumProject.getBookStatus(),
                            albumProject.getBookGeneratedAt()
                    );
                }
                throw new BusinessException(
                        ErrorCode.INVALID_INPUT,
                        "Book already exists but status is not retryable. albumId=" + albumId
                );
            }

            if (albumProject.getBookStatus() == BookGenerationStatus.GENERATED
                    && externalRef.equals(albumProject.getBookExternalRef())) {
                return new GenerationPreparation(
                        albumProject.getId(),
                        albumProject.getTitle(),
                        albumProject.getMonth(),
                        albumProject.getSubtitle(),
                        albumProject.getMonthlyReview(),
                        externalRef,
                        hasPhoto,
                        activityPages,
                        true,
                        albumProject.getBookUid(),
                        albumProject.getBookStatus(),
                        albumProject.getBookGeneratedAt()
                );
            }

            albumProject.markBookGenerationPending(externalRef);
            albumProjectRepository.saveAndFlush(albumProject);

            return new GenerationPreparation(
                    albumProject.getId(),
                    albumProject.getTitle(),
                    albumProject.getMonth(),
                    albumProject.getSubtitle(),
                    albumProject.getMonthlyReview(),
                    externalRef,
                    hasPhoto,
                    activityPages,
                    false,
                    null,
                    albumProject.getBookStatus(),
                    albumProject.getBookGeneratedAt()
            );
        });
    }

    private GenerateBookResponse markGenerated(
            Long userId,
            Long albumId,
            GenerationPreparation preparation,
            GenerateBookRequest request,
            String bookUid
    ) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        return template.execute(status -> {
            AlbumProject albumProject = albumProjectRepository.findByIdAndUserId(albumId, userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ALBUM_NOT_FOUND));

            if (!preparation.externalRef().equals(albumProject.getBookExternalRef())) {
                throw new BusinessException(
                        ErrorCode.SWEETBOOK_CALL_FAILED,
                        "Mismatched externalRef while finalizing book generation."
                );
            }

            if (albumProject.getBookStatus() == BookGenerationStatus.GENERATED
                    && bookUid.equals(albumProject.getBookUid())) {
                return buildGeneratedResponse(new GenerationPreparation(
                        albumProject.getId(),
                        preparation.title(),
                        preparation.month(),
                        preparation.subtitle(),
                        preparation.monthlyReview(),
                        preparation.externalRef(),
                        preparation.hasPhoto(),
                        preparation.activityPages(),
                        true,
                        albumProject.getBookUid(),
                        albumProject.getBookStatus(),
                        albumProject.getBookGeneratedAt()
                ), request.coverTemplateUid(), request.contentTemplateUid());
            }

            LocalDateTime generatedAt = LocalDateTime.now();
            albumProject.markBookGenerated(bookUid, generatedAt);

            return new GenerateBookResponse(
                    albumProject.getId(),
                    bookUid,
                    albumProject.getBookStatus(),
                    preparation.hasPhoto(),
                    request.coverTemplateUid(),
                    MONTH_START_TEMPLATE_UID,
                    request.contentTemplateUid(),
                    preparation.activityPages().size() + 1,
                    generatedAt
            );
        });
    }

    private GenerateBookResponse buildGeneratedResponse(
            GenerationPreparation preparation,
            String coverTemplateUid,
            String contentTemplateUid
    ) {
        return new GenerateBookResponse(
                preparation.albumId(),
                preparation.bookUid(),
                preparation.bookStatus(),
                preparation.hasPhoto(),
                coverTemplateUid,
                MONTH_START_TEMPLATE_UID,
                contentTemplateUid,
                preparation.activityPages().size() + 1,
                preparation.bookGeneratedAt()
        );
    }

    private String resolveExternalRef(AlbumProject albumProject) {
        if (albumProject.getBookExternalRef() != null && !albumProject.getBookExternalRef().isBlank()) {
            return albumProject.getBookExternalRef();
        }
        return "album-" + albumProject.getId();
    }

    private List<ActivityPageData> mapActivityPages(List<AlbumActivity> albumActivities) {
        return albumActivities.stream()
                .map(albumActivity -> {
                    Activity activity = albumActivity.getActivity();
                    return new ActivityPageData(
                            albumActivity.getId(),
                            activity.getActivityDateTime().toString(),
                            activity.getActivityName(),
                            activity.getActivityType(),
                            activity.getDistanceKm(),
                            activity.getMovingTimeSeconds(),
                            albumActivity.getMemo()
                    );
                })
                .toList();
    }

    private Map<String, Object> buildCoverParameters(GenerationPreparation preparation, String title) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("title", title);
        parameters.put("subtitle", preparation.subtitle());
        parameters.put("month", preparation.month());
        return parameters;
    }

    private Map<String, Object> buildEstimateCoverParameters(String title, AlbumProject albumProject) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("title", title);
        parameters.put("subtitle", albumProject.getSubtitle());
        parameters.put("month", albumProject.getMonth());
        return parameters;
    }

    private Map<String, Object> buildMonthStartParameters(GenerationPreparation preparation) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("month", preparation.month());
        parameters.put("monthlyReview", preparation.monthlyReview());
        return parameters;
    }

    private Map<String, Object> buildActivityContentParameters(ActivityPageData activityPage) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("activityDateTime", activityPage.activityDateTime());
        parameters.put("activityName", activityPage.activityName());
        parameters.put("activityType", activityPage.activityType());
        parameters.put("distanceKm", activityPage.distanceKm());
        parameters.put("movingTimeSeconds", activityPage.movingTimeSeconds());
        parameters.put("memo", activityPage.memo());
        return parameters;
    }

    private java.util.Optional<String> resolveUploadedPhotoFileName(String bookUid, Long albumActivityId) {
        List<ActivityPhoto> photos = activityPhotoRepository.findAllByAlbumActivityIdOrderByCreatedAtDesc(albumActivityId);
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

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void validateTemplatesForBookSpec(String bookSpecUid, String coverTemplateUid, String contentTemplateUid) {
        Set<String> coverTemplateUids = sweetbookBooksClient.getTemplates(bookSpecUid, "cover").stream()
                .map(template -> String.valueOf(template.get("templateUid")))
                .filter(uid -> uid != null && !uid.isBlank() && !"null".equals(uid))
                .collect(java.util.stream.Collectors.toSet());

        if (!coverTemplateUids.contains(coverTemplateUid)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "coverTemplateUid is not available for the selected bookSpecUid."
            );
        }

        Set<String> contentTemplateUids = sweetbookBooksClient.getTemplates(bookSpecUid, "content").stream()
                .map(template -> String.valueOf(template.get("templateUid")))
                .filter(uid -> uid != null && !uid.isBlank() && !"null".equals(uid))
                .collect(java.util.stream.Collectors.toSet());

        if (!contentTemplateUids.contains(contentTemplateUid)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "contentTemplateUid is not available for the selected bookSpecUid."
            );
        }

        if (!contentTemplateUids.contains(MONTH_START_TEMPLATE_UID)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "MONTH_START_TEMPLATE_UID is not available for the selected bookSpecUid."
            );
        }
    }

    private record GenerationPreparation(
            Long albumId,
            String title,
            String month,
            String subtitle,
            String monthlyReview,
            String externalRef,
            boolean hasPhoto,
            List<ActivityPageData> activityPages,
            boolean alreadyGenerated,
            String bookUid,
            BookGenerationStatus bookStatus,
            LocalDateTime bookGeneratedAt
    ) {
    }

    private record ActivityPageData(
            Long albumActivityId,
            String activityDateTime,
            String activityName,
            String activityType,
            Double distanceKm,
            Integer movingTimeSeconds,
            String memo
    ) {
    }
}
