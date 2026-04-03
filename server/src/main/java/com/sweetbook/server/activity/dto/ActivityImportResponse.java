package com.sweetbook.server.activity.dto;

import java.util.List;

public record ActivityImportResponse(
        int importedCount,
        int skippedCount,
        List<SkippedRow> skippedRows
) {
    public record SkippedRow(
            long rowNumber,
            String reason
    ) {
    }
}
