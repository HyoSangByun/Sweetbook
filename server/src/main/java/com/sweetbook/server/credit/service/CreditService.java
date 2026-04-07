package com.sweetbook.server.credit.service;

import com.sweetbook.server.credit.dto.ChargeCreditRequest;
import com.sweetbook.server.credit.dto.ChargeCreditResponse;
import com.sweetbook.server.credit.dto.CreditBalanceResponse;
import com.sweetbook.server.sweetbook.client.SweetbookCreditsClient;
import com.sweetbook.server.sweetbook.dto.credits.CreditsBalanceResponseData;
import com.sweetbook.server.sweetbook.dto.credits.SandboxChargeResponseData;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public ChargeCreditResponse chargeSandboxCredits(ChargeCreditRequest request) {
        String normalizedMemo = normalizeMemo(request.memo());
        String idempotencyKey = resolveIdempotencyKey(request, normalizedMemo);
        SandboxChargeResponseData data = sweetbookCreditsClient.chargeSandboxCredits(
                request.amount(),
                normalizedMemo,
                idempotencyKey
        );
        return new ChargeCreditResponse(
                data.transactionUid(),
                data.amount(),
                data.balanceAfter(),
                data.currency()
        );
    }

    private String resolveIdempotencyKey(ChargeCreditRequest request, String normalizedMemo) {
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            return request.idempotencyKey().trim();
        }
        String raw = request.amount() + "|" + normalizedMemo;
        return "credit-charge-" + sha256(raw).substring(0, 24);
    }

    private String normalizeMemo(String memo) {
        return memo == null ? "" : memo.trim();
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", e);
        }
    }
}
