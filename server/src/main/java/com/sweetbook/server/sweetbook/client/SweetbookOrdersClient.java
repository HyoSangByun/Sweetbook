package com.sweetbook.server.sweetbook.client;

import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
import com.sweetbook.server.sweetbook.dto.SweetbookApiResponse;
import com.sweetbook.server.sweetbook.dto.orders.CreateOrderRequest;
import com.sweetbook.server.sweetbook.dto.orders.CreateOrderResponseData;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class SweetbookOrdersClient {

    private final RestClient sweetbookRestClient;

    public String createOrder(CreateOrderRequest request) {
        SweetbookApiResponse<CreateOrderResponseData> response = sweetbookRestClient.post()
                .uri("/v1/orders")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request.payload())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (response == null || !response.success() || response.data() == null || response.data().orderUid() == null) {
            throw new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to create order.");
        }
        return response.data().orderUid();
    }
}

