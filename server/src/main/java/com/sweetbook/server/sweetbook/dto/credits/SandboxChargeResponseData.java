package com.sweetbook.server.sweetbook.dto.credits;

public record SandboxChargeResponseData(
        String transactionUid,
        Long amount,
        Long balanceAfter,
        String currency
) {
}

