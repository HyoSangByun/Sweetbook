package com.sweetbook.server.credit.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ChargeCreditRequest(
        @NotNull(message = "amount는 필수입니다.")
        @Min(value = 1, message = "amount는 1 이상이어야 합니다.")
        Long amount
) {
}
