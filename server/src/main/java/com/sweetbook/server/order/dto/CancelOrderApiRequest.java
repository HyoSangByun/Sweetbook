package com.sweetbook.server.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelOrderApiRequest(
        @NotBlank(message = "cancelReason은 필수입니다.")
        @Size(max = 500, message = "cancelReason은 최대 500자입니다.")
        String cancelReason
) {
}

