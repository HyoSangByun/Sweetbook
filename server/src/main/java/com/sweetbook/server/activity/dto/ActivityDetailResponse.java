package com.sweetbook.server.activity.dto;

import java.time.LocalDateTime;

public record ActivityDetailResponse(
        Long activityId,
        String externalActivityId,
        LocalDateTime activityDateTime,
        String activityMonth,
        String activityType,
        String activityName,
        String description,
        Double distanceKm,
        Integer movingTimeSeconds,
        Integer elapsedTimeSeconds,
        Double averageSpeed,
        Double elevationGain,
        Integer calories
) {
}

