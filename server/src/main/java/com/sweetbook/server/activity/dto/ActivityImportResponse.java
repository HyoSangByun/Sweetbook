package com.sweetbook.server.activity.dto;

public record ActivityImportResponse(
        int importedCount,
        int skippedCount
) {
}

