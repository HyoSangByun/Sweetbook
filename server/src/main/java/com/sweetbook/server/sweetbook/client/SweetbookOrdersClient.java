package com.sweetbook.server.sweetbook.client;

import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
import com.sweetbook.server.sweetbook.dto.SweetbookApiResponse;
import com.sweetbook.server.sweetbook.dto.orders.CreateOrderResponseData;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class SweetbookOrdersClient {

    private final RestClient sweetbookRestClient;

    public String createOrder(Map<String, Object> payload, String idempotencyKey) {
        SweetbookApiResponse<CreateOrderResponseData> response;
        try {
            response = sweetbookRestClient.post()
                    .uri("/v1/orders")
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientException e) {
            BusinessException be = new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to create order.");
            be.initCause(e);
            throw be;
        }

        if (response == null || !response.success() || response.data() == null || response.data().orderUid() == null) {
            throw new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to create order.");
        }
        return response.data().orderUid();
    }
}
