package com.sweetbook.server.album.dto;

public record DeselectAlbumActivityResponse(
        boolean deleted,
        long selectedActivityCount
) {
}

