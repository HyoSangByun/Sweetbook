package com.sweetbook.server.sweetbook.client;

import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
import com.sweetbook.server.sweetbook.dto.SweetbookApiResponse;
import com.sweetbook.server.sweetbook.dto.credits.CreditsBalanceResponseData;
import com.sweetbook.server.sweetbook.dto.credits.SandboxChargeResponseData;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class SweetbookCreditsClient {

    private final RestClient sweetbookRestClient;

    public CreditsBalanceResponseData getCreditsBalance() {
        SweetbookApiResponse<CreditsBalanceResponseData> response;
        try {
            response = sweetbookRestClient.get()
                    .uri("/v1/credits")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientException e) {
            BusinessException be = new BusinessException(
                    ErrorCode.SWEETBOOK_CALL_FAILED,
                    "Failed to fetch credits balance."
            );
            be.initCause(e);
            throw be;
        }

        if (response == null || !response.success() || response.data() == null) {
            throw new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to fetch credits balance.");
        }
        return response.data();
    }

    public SandboxChargeResponseData chargeSandboxCredits(long amount, String memo, String idempotencyKey) {
        SweetbookApiResponse<SandboxChargeResponseData> response;
        try {
            response = sweetbookRestClient.post()
                    .uri("/v1/credits/sandbox/charge")
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "amount", amount,
                            "memo", memo == null ? "" : memo
                    ))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientException e) {
            BusinessException be = new BusinessException(
                    ErrorCode.SWEETBOOK_CALL_FAILED,
                    "Failed to charge sandbox credits."
            );
            be.initCause(e);
            throw be;
        }

        if (response == null || !response.success() || response.data() == null || response.data().transactionUid() == null) {
            throw new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to charge sandbox credits.");
        }
        return response.data();
    }
}

