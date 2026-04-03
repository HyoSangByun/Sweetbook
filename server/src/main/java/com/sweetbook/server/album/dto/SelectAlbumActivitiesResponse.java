package com.sweetbook.server.album.dto;

public record SelectAlbumActivitiesResponse(
        int addedCount,
        int skippedCount,
        long selectedActivityCount
) {
}

