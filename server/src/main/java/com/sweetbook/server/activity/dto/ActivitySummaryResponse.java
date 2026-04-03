package com.sweetbook.server.activity.dto;

import java.time.LocalDateTime;

public record ActivitySummaryResponse(
        Long activityId,
        String externalActivityId,
        LocalDateTime activityDateTime,
        String activityType,
        String activityName,
        Double distanceKm,
        Integer movingTimeSeconds,
        Double averageSpeed,
        Double elevationGain,
        Integer calories
) {
}

