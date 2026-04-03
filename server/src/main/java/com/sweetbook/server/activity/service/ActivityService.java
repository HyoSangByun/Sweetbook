package com.sweetbook.server.activity.service;

import com.sweetbook.server.activity.domain.Activity;
import com.sweetbook.server.activity.dto.ActivityDetailResponse;
import com.sweetbook.server.activity.dto.ActivityMonthResponse;
import com.sweetbook.server.activity.dto.ActivityStatsResponse;
import com.sweetbook.server.activity.dto.ActivitySummaryResponse;
import com.sweetbook.server.activity.repository.ActivityRepository;
import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;

    @Transactional(readOnly = true)
    public List<ActivityMonthResponse> getMonths(Long userId) {
        return activityRepository.findDistinctMonthsByUserId(userId).stream()
                .map(ActivityMonthResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ActivitySummaryResponse> getActivitiesByMonth(Long userId, String month) {
        String normalizedMonth = normalizeMonth(month);
        return activityRepository.findAllByUserIdAndActivityMonthOrderByActivityDateTimeDesc(userId, normalizedMonth).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ActivityDetailResponse getActivityDetail(Long userId, Long activityId) {
        Activity activity = activityRepository.findByIdAndUserId(activityId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND));
        return toDetail(activity);
    }

    @Transactional(readOnly = true)
    public ActivityStatsResponse getMonthlyStats(Long userId, String month) {
        String normalizedMonth = normalizeMonth(month);
        List<Activity> activities = activityRepository.findAllByUserIdAndActivityMonthOrderByActivityDateTimeDesc(
                userId,
                normalizedMonth
        );

        double totalDistanceKm = 0D;
        int totalMovingTimeSeconds = 0;
        int totalCalories = 0;
        Map<String, Long> typeCounts = new LinkedHashMap<>();

        for (Activity activity : activities) {
            if (activity.getDistanceKm() != null) {
                totalDistanceKm += activity.getDistanceKm();
            }
            if (activity.getMovingTimeSeconds() != null) {
                totalMovingTimeSeconds += activity.getMovingTimeSeconds();
            }
            if (activity.getCalories() != null) {
                totalCalories += activity.getCalories();
            }
            typeCounts.merge(activity.getActivityType(), 1L, Long::sum);
        }

        return new ActivityStatsResponse(
                normalizedMonth,
                activities.size(),
                totalDistanceKm,
                totalMovingTimeSeconds,
                totalCalories,
                typeCounts
        );
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

    private ActivitySummaryResponse toSummary(Activity activity) {
        return new ActivitySummaryResponse(
                activity.getId(),
                activity.getExternalActivityId(),
                activity.getActivityDateTime(),
                activity.getActivityType(),
                activity.getActivityName(),
                activity.getDistanceKm(),
                activity.getMovingTimeSeconds(),
                activity.getAverageSpeed(),
                activity.getElevationGain(),
                activity.getCalories()
        );
    }

    private ActivityDetailResponse toDetail(Activity activity) {
        return new ActivityDetailResponse(
                activity.getId(),
                activity.getExternalActivityId(),
                activity.getActivityDateTime(),
                activity.getActivityMonth(),
                activity.getActivityType(),
                activity.getActivityName(),
                activity.getDescription(),
                activity.getDistanceKm(),
                activity.getMovingTimeSeconds(),
                activity.getElapsedTimeSeconds(),
                activity.getAverageSpeed(),
                activity.getElevationGain(),
                activity.getCalories()
        );
    }
}

