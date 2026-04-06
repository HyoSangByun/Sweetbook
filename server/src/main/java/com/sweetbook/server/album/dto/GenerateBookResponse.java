package com.sweetbook.server.album.dto;

import com.sweetbook.server.album.domain.BookGenerationStatus;
import java.time.LocalDateTime;

public record GenerateBookResponse(
        Long albumId,
        String bookUid,
        BookGenerationStatus bookStatus,
        boolean hasPhoto,
        String coverTemplateUid,
        String monthStartTemplateUid,
        String contentTemplateUid,
        int generatedPageCount,
        LocalDateTime bookGeneratedAt
) {
}

