package com.sweetbook.server.album.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AddBookContentsRequest(
        @NotEmpty(message = "pages는 최소 1개 이상이어야 합니다.")
        List<@NotNull @Valid ContentPageInput> pages
) {
    public record ContentPageInput(
            @NotNull(message = "albumActivityId는 필수입니다.")
            Long albumActivityId,
            List<String> photoFileNames
    ) {
    }
}

