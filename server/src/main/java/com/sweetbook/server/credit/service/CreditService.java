package com.sweetbook.server.credit.service;

import com.sweetbook.server.credit.dto.CreditBalanceResponse;
import com.sweetbook.server.credit.dto.ChargeCreditResponse;
import com.sweetbook.server.sweetbook.client.SweetbookCreditsClient;
import com.sweetbook.server.sweetbook.dto.credits.CreditsBalanceResponseData;
import com.sweetbook.server.sweetbook.dto.credits.SandboxChargeResponseData;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditService {

    private final SweetbookCreditsClient sweetbookCreditsClient;

    public CreditBalanceResponse getCreditsBalance() {
        CreditsBalanceResponseData data = sweetbookCreditsClient.getCreditsBalance();
        return new CreditBalanceResponse(
                data.accountUid(),
                data.balance(),
                data.currency(),
                data.env(),
                data.createdAt() == null ? null : data.createdAt().toLocalDateTime(),
                data.updatedAt() == null ? null : data.updatedAt().toLocalDateTime()
        );
    }

    public ChargeCreditResponse chargeSandboxCredits(long amount) {
        String idempotencyKey = UUID.randomUUID().toString();
        log.info("Charging sandbox credits. amount={}, idempotencyKey={}", amount, idempotencyKey);
        SandboxChargeResponseData data = sweetbookCreditsClient.chargeSandboxCredits(
                amount,
                idempotencyKey
        );

        CreditsBalanceResponseData balance = sweetbookCreditsClient.getCreditsBalance();
        Long resolvedAmount = data.amount() != null ? data.amount() : amount;
        Long resolvedBalanceAfter = data.balanceAfter() != null ? data.balanceAfter() : balance.balance();
        String resolvedCurrency = data.currency() != null ? data.currency() : balance.currency();

        return new ChargeCreditResponse(
                data.transactionUid(),
                resolvedAmount,
                resolvedBalanceAfter,
                resolvedCurrency
        );
    }
}
