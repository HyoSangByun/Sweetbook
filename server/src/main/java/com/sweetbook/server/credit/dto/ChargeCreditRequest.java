package com.sweetbook.server.credit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ChargeCreditRequest(
        @NotNull(message = "amount는 필수입니다.")
        @Positive(message = "amount는 양수여야 합니다.")
        Long amount,
        @Size(max = 200, message = "memo는 최대 200자입니다.")
        String memo,
        @Size(max = 120, message = "idempotencyKey는 최대 120자입니다.")
        String idempotencyKey
) {
}

