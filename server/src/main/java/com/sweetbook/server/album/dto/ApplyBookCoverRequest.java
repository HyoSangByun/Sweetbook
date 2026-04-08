package com.sweetbook.server.album.dto;

import jakarta.validation.constraints.NotBlank;

public record ApplyBookCoverRequest(
        @NotBlank(message = "coverPhotoFileName은 필수입니다.")
        String coverPhotoFileName,
        String subtitle
) {
}
