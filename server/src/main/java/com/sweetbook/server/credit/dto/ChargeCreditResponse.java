package com.sweetbook.server.credit.dto;

public record ChargeCreditResponse(
        String transactionUid,
        long amount,
        long balanceAfter,
        String currency
) {
}
