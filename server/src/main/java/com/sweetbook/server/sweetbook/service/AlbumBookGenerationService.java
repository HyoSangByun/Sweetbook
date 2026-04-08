package com.sweetbook.server.sweetbook.service;

import com.sweetbook.server.activity.domain.Activity;
import com.sweetbook.server.album.domain.AlbumActivity;
import com.sweetbook.server.album.domain.AlbumProject;
import com.sweetbook.server.album.dto.AddBookContentsRequest;
import com.sweetbook.server.album.dto.ApplyBookCoverRequest;
import com.sweetbook.server.album.dto.CreateBookDraftRequest;
import com.sweetbook.server.album.dto.CreateBookDraftResponse;
import com.sweetbook.server.album.dto.FinalizeBookResponse;
import com.sweetbook.server.album.dto.UploadBookPhotoResponse;
import com.sweetbook.server.album.repository.AlbumActivityRepository;
import com.sweetbook.server.album.repository.AlbumProjectRepository;
import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
import com.sweetbook.server.sweetbook.client.SweetbookBooksClient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AlbumBookGenerationService {

    private static final String FIXED_BOOK_SPEC_UID = "SQUAREBOOK_HC";
    private static final String FIXED_COVER_TEMPLATE_UID = "4Fy1mpIlm1ek";
    private static final String FIXED_CONTENT_TEMPLATE_UID = "3T09l6GEd0AL";
    private static final String DEFAULT_CONTENT_PLACEHOLDER_IMAGE_URL = "https://placehold.co/300x200.jpg";
    private static final int MIN_ACTIVITY_COUNT = 24;
    private static final DateTimeFormatter DATE_RANGE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final AlbumProjectRepository albumProjectRepository;
    private final AlbumActivityRepository albumActivityRepository;
    private final SweetbookBooksClient sweetbookBooksClient;
    private final SweetbookBookPhotoSelector sweetbookBookPhotoSelector;
    private final SweetbookTemplateMetadataResolver sweetbookTemplateMetadataResolver;

    @Transactional
    public CreateBookDraftResponse createDraftBook(Long albumId, CreateBookDraftRequest request) {
        AlbumProject albumProject = getOwnedAlbum(albumId);
        List<AlbumActivity> activities = albumActivityRepository.findAllByAlbumProjectIdOrderByActivityActivityDateTimeDesc(albumId);

        if (activities.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "No selected activities. Cannot create book.");
        }
        if (activities.size() < MIN_ACTIVITY_COUNT) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "At least " + MIN_ACTIVITY_COUNT + " activities are required to create a book."
            );
        }

        String externalRef = "album-" + albumId + "-" + UUID.randomUUID();
        String bookUid = sweetbookBooksClient.createBook(request.title().trim(), FIXED_BOOK_SPEC_UID, externalRef);
        albumProject.markBookGenerationPending(bookUid, externalRef);
        return new CreateBookDraftResponse(albumId, bookUid);
    }

    @Transactional(readOnly = true)
    public UploadBookPhotoResponse uploadBookPhoto(Long albumId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Upload file is empty.");
        }
        AlbumProject albumProject = getOwnedAlbum(albumId);
        String bookUid = requireBookUid(albumProject);
        String fileName = sweetbookBooksClient.uploadPhoto(bookUid, file);
        return new UploadBookPhotoResponse(fileName);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listBookPhotos(Long albumId) {
        AlbumProject albumProject = getOwnedAlbum(albumId);
        String bookUid = requireBookUid(albumProject);
        return sweetbookBooksClient.getBookPhotos(bookUid);
    }

    @Transactional(readOnly = true)
    public void applyCover(Long albumId, ApplyBookCoverRequest request) {
        AlbumProject albumProject = getOwnedAlbum(albumId);
        String bookUid = requireBookUid(albumProject);

        Set<String> selectableFileNames = sweetbookBookPhotoSelector.listSelectableFileNames(bookUid);
        sweetbookBookPhotoSelector.validateSelectedFileName(selectableFileNames, request.coverPhotoFileName(), "coverPhotoFileName");
        resolveFixedCoverTemplate();

        List<AlbumActivity> albumActivities = albumActivityRepository.findAllByAlbumProjectIdOrderByActivityActivityDateTimeDesc(albumId);
        if (albumActivities.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "No selected activities. Cannot build dateRange.");
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("coverPhoto", request.coverPhotoFileName().trim());
        params.put("subtitle", resolveCoverSubtitle(request.subtitle(), albumProject.getTitle()));
        params.put("dateRange", buildDateRange(albumActivities));
        sweetbookBooksClient.addCover(bookUid, FIXED_COVER_TEMPLATE_UID, params);
    }

    @Transactional(readOnly = true)
    public void addContents(Long albumId, AddBookContentsRequest request) {
        AlbumProject albumProject = getOwnedAlbum(albumId);
        String bookUid = requireBookUid(albumProject);
        Map<Long, List<String>> photoFileNamesByAlbumActivityId = toPagePhotoMap(request.pages());

        List<AlbumActivity> albumActivities = albumActivityRepository.findAllByAlbumProjectIdOrderByActivityActivityDateTimeDesc(albumId);
        if (albumActivities.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "No selected activities. Cannot add contents.");
        }
        if (albumActivities.size() < MIN_ACTIVITY_COUNT) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "At least " + MIN_ACTIVITY_COUNT + " activities are required to add contents."
            );
        }

        Set<String> selectableFileNames = sweetbookBookPhotoSelector.listSelectableFileNames(bookUid);
        Map<String, Object> contentTemplateDetail = sweetbookBooksClient.getTemplateDetail(FIXED_CONTENT_TEMPLATE_UID);
        Map<String, Object> definitions = sweetbookTemplateMetadataResolver.extractDefinitions(contentTemplateDetail);
        Set<String> requiredKeys = sweetbookTemplateMetadataResolver.resolveRequiredKeys(definitions);

        if (!requiredKeys.contains("dayLabel") || !requiredKeys.contains("photos")) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Required content template parameters are missing: dayLabel, photos."
            );
        }

        for (AlbumActivity albumActivity : albumActivities) {
            Activity activity = albumActivity.getActivity();
            List<String> requestedPhotoSources = photoFileNamesByAlbumActivityId.getOrDefault(albumActivity.getId(), List.of());
            List<String> photoFileNames = requestedPhotoSources.isEmpty()
                    ? List.of(DEFAULT_CONTENT_PLACEHOLDER_IMAGE_URL)
                    : requestedPhotoSources;

            for (String fileName : photoFileNames) {
                if (!isHttpUrl(fileName)) {
                    sweetbookBookPhotoSelector.validateSelectedFileName(selectableFileNames, fileName, "photos");
                }
            }

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("dayLabel", buildDayLabel(activity));
            params.put("photos", photoFileNames);
            sweetbookTemplateMetadataResolver.putIfTemplateParameterExists(params, definitions, "activityName", activity.getActivityName());
            sweetbookTemplateMetadataResolver.putIfTemplateParameterExists(params, definitions, "activityType", activity.getActivityType());
            sweetbookTemplateMetadataResolver.putIfTemplateParameterExists(params, definitions, "distanceKm", activity.getDistanceKm());
            sweetbookTemplateMetadataResolver.putIfTemplateParameterExists(params, definitions, "memo", albumActivity.getMemo());

            sweetbookBooksClient.addContent(bookUid, FIXED_CONTENT_TEMPLATE_UID, params, "page");
        }
    }

    @Transactional
    public FinalizeBookResponse finalizeBook(Long albumId) {
        AlbumProject albumProject = getOwnedAlbum(albumId);
        String bookUid = requireBookUid(albumProject);
        sweetbookBooksClient.finalizeBook(bookUid);
        LocalDateTime generatedAt = LocalDateTime.now();
        albumProject.markBookGenerated(bookUid, generatedAt);
        return new FinalizeBookResponse(albumId, bookUid, albumProject.getBookStatus(), generatedAt);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAlbumBooks(Long albumId) {
        getOwnedAlbum(albumId);
        List<Map<String, Object>> books = sweetbookBooksClient.getBooks(100, 0);
        String prefix = "album-" + albumId + "-";

        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> book : books) {
            Object externalRef = book.get("externalRef");
            if (externalRef != null && String.valueOf(externalRef).startsWith(prefix)) {
                filtered.add(book);
            }
        }
        return filtered;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAllUserBooks() {
        return sweetbookBooksClient.getBooks(100, 0);
    }

    private Map<Long, List<String>> toPagePhotoMap(List<AddBookContentsRequest.ContentPageInput> pages) {
        Map<Long, List<String>> result = new HashMap<>();
        for (AddBookContentsRequest.ContentPageInput page : pages) {
            List<String> names = page.photoFileNames() == null
                    ? List.of()
                    : page.photoFileNames().stream()
                    .filter(name -> name != null && !name.isBlank())
                    .map(String::trim)
                    .toList();
            result.put(page.albumActivityId(), names);
        }
        return result;
    }

    private AlbumProject getOwnedAlbum(Long albumId) {
        return albumProjectRepository.findById(albumId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALBUM_NOT_FOUND));
    }

    private String requireBookUid(AlbumProject albumProject) {
        String bookUid = albumProject.getBookUid();
        if (bookUid == null || bookUid.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Book is not created yet. Run POST /books first.");
        }
        return bookUid;
    }

    private String buildDayLabel(Activity activity) {
        String date = activity.getActivityDateTime().toLocalDate().toString();
        String distance = activity.getDistanceKm() == null
                ? "0.00km"
                : String.format(Locale.US, "%.2fkm", activity.getDistanceKm());
        return date + " - " + activity.getActivityName() + " - " + distance;
    }

    private String resolveCoverSubtitle(String subtitle, String fallbackTitle) {
        if (subtitle != null && !subtitle.isBlank()) {
            return subtitle.trim();
        }
        return fallbackTitle == null ? "" : fallbackTitle.trim();
    }

    private String buildDateRange(List<AlbumActivity> albumActivities) {
        LocalDate minDate = null;
        LocalDate maxDate = null;
        for (AlbumActivity albumActivity : albumActivities) {
            LocalDate date = albumActivity.getActivity().getActivityDateTime().toLocalDate();
            if (minDate == null || date.isBefore(minDate)) {
                minDate = date;
            }
            if (maxDate == null || date.isAfter(maxDate)) {
                maxDate = date;
            }
        }

        if (minDate == null || maxDate == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Cannot calculate activity date range.");
        }
        return DATE_RANGE_FORMATTER.format(minDate) + " - " + DATE_RANGE_FORMATTER.format(maxDate);
    }

    private Map<String, Object> resolveFixedCoverTemplate() {
        List<Map<String, Object>> coverTemplates = sweetbookBooksClient.getTemplates(FIXED_BOOK_SPEC_UID, "cover");
        for (Map<String, Object> template : coverTemplates) {
            Object uid = template.get("templateUid");
            if (uid != null && FIXED_COVER_TEMPLATE_UID.equals(String.valueOf(uid))) {
                return template;
            }
        }

        String available = coverTemplates.stream()
                .map(template -> template.get("templateUid"))
                .filter(uid -> uid != null)
                .map(String::valueOf)
                .reduce((a, b) -> a + ", " + b)
                .orElse("(none)");

        throw new BusinessException(
                ErrorCode.INVALID_INPUT,
                "Fixed cover template UID is not available in current environment. requested=" + FIXED_COVER_TEMPLATE_UID
                        + ", available=[" + available + "]"
        );
    }

    private boolean isHttpUrl(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }
}
