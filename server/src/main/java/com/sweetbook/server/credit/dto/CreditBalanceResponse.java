package com.sweetbook.server.credit.dto;

import java.time.LocalDateTime;

public record CreditBalanceResponse(
        String accountUid,
        Long balance,
        String currency,
        String env,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

