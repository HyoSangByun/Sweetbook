package com.sweetbook.server.album.dto;

import com.sweetbook.server.album.domain.BookGenerationStatus;
import java.time.LocalDateTime;

public record FinalizeBookResponse(
        Long albumId,
        String bookUid,
        BookGenerationStatus bookStatus,
        LocalDateTime finalizedAt
) {
}

