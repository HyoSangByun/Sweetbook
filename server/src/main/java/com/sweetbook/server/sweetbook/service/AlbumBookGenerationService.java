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
import java.util.LinkedHashSet;
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
    private static final int MIN_ACTIVITY_COUNT = 24;
    private static final DateTimeFormatter DATE_RANGE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final AlbumProjectRepository albumProjectRepository;
    private final AlbumActivityRepository albumActivityRepository;
    private final SweetbookBooksClient sweetbookBooksClient;

    @Transactional
    public CreateBookDraftResponse createDraftBook(Long userId, Long albumId, CreateBookDraftRequest request) {
        AlbumProject albumProject = getOwnedAlbum(userId, albumId);
        List<AlbumActivity> activities = albumActivityRepository.findAllByAlbumProjectIdOrderByActivityActivityDateTimeDesc(albumId);
        if (activities.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "선택된 활동이 없어 책을 생성할 수 없습니다.");
        }
        if (activities.size() < MIN_ACTIVITY_COUNT) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "책 생성을 위해 활동을 최소 " + MIN_ACTIVITY_COUNT + "개 이상 선택해야 합니다."
            );
        }

        String externalRef = "album-" + albumId + "-" + UUID.randomUUID();
        String bookUid = sweetbookBooksClient.createBook(request.title().trim(), FIXED_BOOK_SPEC_UID, externalRef);
        albumProject.markBookGenerationPending(bookUid, externalRef);
        return new CreateBookDraftResponse(albumId, bookUid);
    }

    @Transactional(readOnly = true)
    public UploadBookPhotoResponse uploadBookPhoto(Long userId, Long albumId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "업로드 파일이 비어 있습니다.");
        }
        AlbumProject albumProject = getOwnedAlbum(userId, albumId);
        String bookUid = requireBookUid(albumProject);
        String fileName = sweetbookBooksClient.uploadPhoto(bookUid, file);
        return new UploadBookPhotoResponse(fileName);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listBookPhotos(Long userId, Long albumId) {
        AlbumProject albumProject = getOwnedAlbum(userId, albumId);
        String bookUid = requireBookUid(albumProject);
        return sweetbookBooksClient.getBookPhotos(bookUid);
    }

    @Transactional(readOnly = true)
    public void applyCover(Long userId, Long albumId, ApplyBookCoverRequest request) {
        AlbumProject albumProject = getOwnedAlbum(userId, albumId);
        String bookUid = requireBookUid(albumProject);
        Set<String> selectableFileNames = listSelectableFileNames(bookUid);
        validateSelectedFileName(selectableFileNames, request.coverPhotoFileName(), "coverPhotoFileName");
        resolveFixedCoverTemplate();
        List<AlbumActivity> albumActivities = albumActivityRepository.findAllByAlbumProjectIdOrderByActivityActivityDateTimeDesc(albumId);
        if (albumActivities.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "선택된 활동이 없어 표지 dateRange를 만들 수 없습니다.");
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("coverPhoto", request.coverPhotoFileName().trim());
        params.put("subtitle", resolveCoverSubtitle(request.subtitle(), albumProject.getTitle()));
        params.put("dateRange", buildDateRange(albumActivities));
        sweetbookBooksClient.addCover(bookUid, FIXED_COVER_TEMPLATE_UID, params);
    }

    @Transactional(readOnly = true)
    public void addContents(Long userId, Long albumId, AddBookContentsRequest request) {
        AlbumProject albumProject = getOwnedAlbum(userId, albumId);
        String bookUid = requireBookUid(albumProject);
        Map<Long, List<String>> photoFileNamesByAlbumActivityId = toPagePhotoMap(request.pages());

        List<AlbumActivity> albumActivities = albumActivityRepository.findAllByAlbumProjectIdOrderByActivityActivityDateTimeDesc(albumId);
        if (albumActivities.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "선택된 활동이 없어 내지를 추가할 수 없습니다.");
        }
        if (albumActivities.size() < MIN_ACTIVITY_COUNT) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "내지 추가를 위해 활동을 최소 " + MIN_ACTIVITY_COUNT + "개 이상 선택해야 합니다."
            );
        }

        Set<String> selectableFileNames = listSelectableFileNames(bookUid);
        Map<String, Object> contentTemplateDetail = sweetbookBooksClient.getTemplateDetail(FIXED_CONTENT_TEMPLATE_UID);
        Map<String, Object> definitions = extractTemplateParameterDefinitions(contentTemplateDetail);
        Set<String> requiredKeys = resolveRequiredKeys(definitions);
        if (!requiredKeys.contains("dayLabel") || !requiredKeys.contains("photos")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "고정 내지 템플릿의 필수 파라미터(dayLabel, photos)가 누락되었습니다.");
        }

        for (AlbumActivity albumActivity : albumActivities) {
            Activity activity = albumActivity.getActivity();
            List<String> photoFileNames = photoFileNamesByAlbumActivityId.getOrDefault(albumActivity.getId(), List.of());
            for (String fileName : photoFileNames) {
                validateSelectedFileName(selectableFileNames, fileName, "photos");
            }

            if (photoFileNames.isEmpty()) {
                throw new BusinessException(
                        ErrorCode.INVALID_INPUT,
                        "활동별 내지 사진은 필수입니다. albumActivityId=" + albumActivity.getId()
                );
            }

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("dayLabel", buildDayLabel(activity));
            params.put("photos", photoFileNames);
            putIfTemplateParameterExists(params, definitions, "activityName", activity.getActivityName());
            putIfTemplateParameterExists(params, definitions, "activityType", activity.getActivityType());
            putIfTemplateParameterExists(params, definitions, "distanceKm", activity.getDistanceKm());
            putIfTemplateParameterExists(params, definitions, "memo", albumActivity.getMemo());
            sweetbookBooksClient.addContent(bookUid, FIXED_CONTENT_TEMPLATE_UID, params, "page");
        }
    }

    @Transactional
    public FinalizeBookResponse finalizeBook(Long userId, Long albumId) {
        AlbumProject albumProject = getOwnedAlbum(userId, albumId);
        String bookUid = requireBookUid(albumProject);
        sweetbookBooksClient.finalizeBook(bookUid);
        LocalDateTime generatedAt = LocalDateTime.now();
        albumProject.markBookGenerated(bookUid, generatedAt);
        return new FinalizeBookResponse(albumId, bookUid, albumProject.getBookStatus(), generatedAt);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAlbumBooks(Long userId, Long albumId) {
        getOwnedAlbum(userId, albumId);
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

    private AlbumProject getOwnedAlbum(Long userId, Long albumId) {
        return albumProjectRepository.findByIdAndUserId(albumId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALBUM_NOT_FOUND));
    }

    private String requireBookUid(AlbumProject albumProject) {
        String bookUid = albumProject.getBookUid();
        if (bookUid == null || bookUid.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "책이 아직 생성되지 않았습니다. 먼저 POST /books 단계를 진행하세요.");
        }
        return bookUid;
    }

    private Set<String> listSelectableFileNames(String bookUid) {
        return sweetbookBooksClient.getBookPhotos(bookUid).stream()
                .map(photo -> photo.get("fileName"))
                .filter(value -> value != null)
                .map(String::valueOf)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private void validateSelectedFileName(Set<String> selectableFileNames, String fileName, String fieldName) {
        String normalized = fileName == null ? "" : fileName.trim();
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " 값이 비어 있습니다.");
        }
        if (!selectableFileNames.contains(normalized)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    fieldName + "는 GET /books/{bookUid}/photos 에서 선택 가능한 fileName 이어야 합니다. value=" + normalized
            );
        }
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
            throw new BusinessException(ErrorCode.INVALID_INPUT, "활동 날짜 범위를 계산할 수 없습니다.");
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
                "고정 커버 템플릿 UID를 현재 환경에서 찾을 수 없습니다. requested=" + FIXED_COVER_TEMPLATE_UID
                        + ", available=[" + available + "]"
        );
    }

    private Map<String, Object> extractTemplateParameterDefinitions(Map<String, Object> templateDetail) {
        Object templateObject = templateDetail.get("template");
        Map<String, Object> templateRoot = templateDetail;
        if (templateObject instanceof Map<?, ?> templateMap) {
            templateRoot = toStringKeyMap(templateMap);
        }
        Object parametersObject = templateRoot.get("parameters");
        if (!(parametersObject instanceof Map<?, ?> parametersMap)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Template parameters metadata is missing.");
        }
        Object definitionsObject = parametersMap.get("definitions");
        if (!(definitionsObject instanceof Map<?, ?> definitionsMap)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Template parameter definitions are missing.");
        }
        return toStringKeyMap(definitionsMap);
    }

    private Map<String, Object> toStringKeyMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private Set<String> resolveRequiredKeys(Map<String, Object> definitions) {
        Set<String> requiredKeys = new LinkedHashSet<>();
        for (Map.Entry<String, Object> entry : definitions.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> map && Boolean.TRUE.equals(map.get("required"))) {
                requiredKeys.add(entry.getKey());
            }
        }
        return requiredKeys;
    }

    private void putIfTemplateParameterExists(
            Map<String, Object> params,
            Map<String, Object> definitions,
            String key,
            Object value
    ) {
        if (definitions.containsKey(key) && value != null) {
            params.put(key, value);
        }
    }

}
