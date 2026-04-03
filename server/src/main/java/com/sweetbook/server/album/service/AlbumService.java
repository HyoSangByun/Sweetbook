package com.sweetbook.server.album.service;

import com.sweetbook.server.activity.domain.Activity;
import com.sweetbook.server.activity.repository.ActivityRepository;
import com.sweetbook.server.album.domain.AlbumActivity;
import com.sweetbook.server.album.domain.AlbumProject;
import com.sweetbook.server.album.domain.AlbumProjectStatus;
import com.sweetbook.server.album.dto.AlbumActivityItemResponse;
import com.sweetbook.server.album.dto.AlbumResponse;
import com.sweetbook.server.album.dto.CreateAlbumRequest;
import com.sweetbook.server.album.dto.DeselectAlbumActivityResponse;
import com.sweetbook.server.album.dto.SelectAlbumActivitiesRequest;
import com.sweetbook.server.album.dto.SelectAlbumActivitiesResponse;
import com.sweetbook.server.album.dto.UpdateAlbumRequest;
import com.sweetbook.server.album.repository.AlbumActivityRepository;
import com.sweetbook.server.album.repository.AlbumProjectRepository;
import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
import com.sweetbook.server.photo.service.ActivityPhotoService;
import com.sweetbook.server.photo.repository.ActivityPhotoRepository;
import com.sweetbook.server.user.domain.User;
import com.sweetbook.server.user.repository.UserRepository;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlbumService {

    private final AlbumProjectRepository albumProjectRepository;
    private final AlbumActivityRepository albumActivityRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final ActivityPhotoRepository activityPhotoRepository;
    private final ActivityPhotoService activityPhotoService;

    @Transactional
    public AlbumResponse createAlbum(Long userId, CreateAlbumRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String normalizedMonth = normalizeMonth(request.month());
        AlbumProject albumProject = AlbumProject.builder()
                .user(user)
                .month(normalizedMonth)
                .title(request.title().trim())
                .subtitle(request.subtitle())
                .monthlyReview(request.monthlyReview())
                .status(AlbumProjectStatus.DRAFT)
                .build();

        AlbumProject saved = albumProjectRepository.save(albumProject);
        return toResponse(saved, List.of(), false);
    }

    @Transactional(readOnly = true)
    public AlbumResponse getAlbum(Long userId, Long albumId) {
        AlbumProject albumProject = getOwnedAlbum(userId, albumId);
        List<AlbumActivity> albumActivities =
                albumActivityRepository.findAllByAlbumProjectIdOrderByActivityActivityDateTimeDesc(albumId);
        boolean hasPhoto = hasAnyPhotoInAlbumInternal(albumProject.getId());
        return toResponse(albumProject, albumActivities, hasPhoto);
    }

    @Transactional
    public AlbumResponse updateAlbum(Long userId, Long albumId, UpdateAlbumRequest request) {
        AlbumProject albumProject = getOwnedAlbum(userId, albumId);
        albumProject.update(request.title(), request.subtitle(), request.monthlyReview());
        List<AlbumActivity> albumActivities =
                albumActivityRepository.findAllByAlbumProjectIdOrderByActivityActivityDateTimeDesc(albumId);
        boolean hasPhoto = hasAnyPhotoInAlbumInternal(albumProject.getId());
        return toResponse(albumProject, albumActivities, hasPhoto);
    }

    @Transactional
    public SelectAlbumActivitiesResponse selectAlbumActivities(
            Long userId,
            Long albumId,
            SelectAlbumActivitiesRequest request
    ) {
        AlbumProject albumProject = getOwnedAlbum(userId, albumId);
        Set<Long> distinctActivityIds = new LinkedHashSet<>(request.activityIds());

        int addedCount = 0;
        int skippedCount = 0;

        for (Long activityId : distinctActivityIds) {
            Activity activity = activityRepository.findByIdAndUserId(activityId, userId)
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.ACTIVITY_NOT_FOUND,
                            "activityId=" + activityId
                    ));

            try {
                int inserted = albumActivityRepository.insertAlbumActivity(albumProject.getId(), activity.getId());
                if (inserted > 0) {
                    addedCount++;
                } else {
                    skippedCount++;
                }
            } catch (DataIntegrityViolationException ex) {
                skippedCount++;
            }
        }

        long selectedActivityCount = albumActivityRepository.countByAlbumProjectId(albumProject.getId());
        return new SelectAlbumActivitiesResponse(addedCount, skippedCount, selectedActivityCount);
    }

    @Transactional
    public DeselectAlbumActivityResponse deselectAlbumActivity(Long userId, Long albumId, Long activityId) {
        AlbumProject albumProject = getOwnedAlbum(userId, albumId);
        AlbumActivity albumActivity = albumActivityRepository
                .findByAlbumProjectIdAndActivityId(albumProject.getId(), activityId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ALBUM_ACTIVITY_NOT_FOUND,
                        "albumId=" + albumId + ", activityId=" + activityId
                ));

        activityPhotoService.deleteAllForAlbumActivity(albumActivity);
        albumActivityRepository.delete(albumActivity);
        long selectedActivityCount = albumActivityRepository.countByAlbumProjectId(albumProject.getId());
        return new DeselectAlbumActivityResponse(true, selectedActivityCount);
    }

    private AlbumProject getOwnedAlbum(Long userId, Long albumId) {
        return albumProjectRepository.findByIdAndUserId(albumId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALBUM_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public boolean hasAnyPhotoInAlbum(Long userId, Long albumId) {
        AlbumProject albumProject = getOwnedAlbum(userId, albumId);
        return hasAnyPhotoInAlbumInternal(albumProject.getId());
    }

    private String normalizeMonth(String month) {
        if (month == null || month.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "month 파라미터는 필수입니다. (예: 2026-04)");
        }
        try {
            return YearMonth.parse(month).toString();
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "month 형식은 YYYY-MM 이어야 합니다.");
        }
    }

    private boolean hasAnyPhotoInAlbumInternal(Long albumId) {
        return activityPhotoRepository.existsByAlbumActivityAlbumProjectId(albumId);
    }

    private AlbumResponse toResponse(AlbumProject albumProject, List<AlbumActivity> albumActivities, boolean hasPhoto) {
        List<AlbumActivityItemResponse> selectedActivities = new ArrayList<>();
        for (AlbumActivity albumActivity : albumActivities) {
            Activity activity = albumActivity.getActivity();
            selectedActivities.add(new AlbumActivityItemResponse(
                    albumActivity.getId(),
                    activity.getId(),
                    activity.getExternalActivityId(),
                    activity.getActivityDateTime(),
                    activity.getActivityType(),
                    activity.getActivityName(),
                    activity.getDistanceKm(),
                    activity.getMovingTimeSeconds(),
                    albumActivity.getMemo()
            ));
        }

        return new AlbumResponse(
                albumProject.getId(),
                albumProject.getMonth(),
                albumProject.getTitle(),
                albumProject.getSubtitle(),
                albumProject.getMonthlyReview(),
                albumProject.getStatus(),
                hasPhoto,
                selectedActivities.size(),
                selectedActivities,
                albumProject.getCreatedAt(),
                albumProject.getUpdatedAt()
        );
    }
}
