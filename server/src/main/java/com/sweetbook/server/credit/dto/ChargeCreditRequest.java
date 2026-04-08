package com.sweetbook.server.credit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChargeCreditRequest(
        @NotNull(message = "amount는 필수입니다.")
        @Positive(message = "amount는 양수여야 합니다.")
        Long amount
) {
}
