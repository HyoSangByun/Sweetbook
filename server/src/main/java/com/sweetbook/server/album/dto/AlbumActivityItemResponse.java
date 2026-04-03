package com.sweetbook.server.album.dto;

import java.time.LocalDateTime;

public record AlbumActivityItemResponse(
        Long albumActivityId,
        Long activityId,
        String externalActivityId,
        LocalDateTime activityDateTime,
        String activityType,
        String activityName,
        Double distanceKm,
        Integer movingTimeSeconds,
        String memo
) {
}

