package com.sweetbook.server.album.dto;

import jakarta.validation.constraints.Size;

public record UpdateAlbumRequest(
        @Size(max = 150, message = "title은 150자 이하여야 합니다.")
        String title,

        @Size(max = 300, message = "subtitle은 300자 이하여야 합니다.")
        String subtitle,

        @Size(max = 4000, message = "monthlyReview는 4000자 이하여야 합니다.")
        String monthlyReview
) {
}

