package com.sweetbook.server.credit.dto;

public record ChargeCreditResponse(
        String transactionUid,
        Long amount,
        Long balanceAfter,
        String currency
) {
}

