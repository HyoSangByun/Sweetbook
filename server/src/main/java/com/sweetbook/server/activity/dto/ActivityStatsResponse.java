package com.sweetbook.server.activity.dto;

import java.util.Map;

public record ActivityStatsResponse(
        String month,
        long totalCount,
        double totalDistanceKm,
        int totalMovingTimeSeconds,
        int totalCalories,
        Map<String, Long> activityTypeCounts
) {
}

