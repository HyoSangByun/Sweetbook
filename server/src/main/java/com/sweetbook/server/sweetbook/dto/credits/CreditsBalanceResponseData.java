package com.sweetbook.server.sweetbook.dto.credits;

import java.time.OffsetDateTime;

public record CreditsBalanceResponseData(
        String accountUid,
        Long balance,
        String currency,
        String env,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

