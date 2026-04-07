package com.sweetbook.server.album.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BookEstimateRequest(
        @NotBlank(message = "title은 필수입니다.")
        @Size(max = 255, message = "title은 최대 255자입니다.")
        String title,
        @NotBlank(message = "bookSpecUid는 필수입니다.")
        String bookSpecUid,
        @NotBlank(message = "coverTemplateUid는 필수입니다.")
        String coverTemplateUid,
        @NotBlank(message = "contentTemplateUid는 필수입니다.")
        String contentTemplateUid
) {
}
