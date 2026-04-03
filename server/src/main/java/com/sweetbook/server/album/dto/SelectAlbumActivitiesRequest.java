package com.sweetbook.server.album.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SelectAlbumActivitiesRequest(
        @NotEmpty(message = "activityIds는 최소 1개 이상이어야 합니다.")
        List<Long> activityIds
) {
}

