package com.sweetbook.server.album.dto;

import com.sweetbook.server.album.domain.AlbumProjectStatus;
import com.sweetbook.server.album.domain.BookGenerationStatus;
import java.time.LocalDateTime;
import java.util.List;

public record AlbumResponse(
        Long albumId,
        String month,
        String title,
        String subtitle,
        String monthlyReview,
        AlbumProjectStatus status,
        String bookUid,
        BookGenerationStatus bookStatus,
        LocalDateTime bookGeneratedAt,
        boolean hasPhoto,
        long selectedActivityCount,
        List<AlbumActivityItemResponse> selectedActivities,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
