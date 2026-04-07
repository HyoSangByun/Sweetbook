package com.sweetbook.server.sweetbook.client;

import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
import com.sweetbook.server.sweetbook.dto.SweetbookApiResponse;
import com.sweetbook.server.sweetbook.dto.orders.CreateOrderResponseData;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private static final ParameterizedTypeReference<SweetbookApiResponse<Map<String, Object>>> MAP_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    public String createOrder(Map<String, Object> payload) {
        SweetbookApiResponse<CreateOrderResponseData> response;
        try {
            response = sweetbookRestClient.post()
                    .uri("/v1/orders")
                    .header("Idempotency-Key", UUID.randomUUID().toString())
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

    public Map<String, Object> estimateOrder(String bookUid, int quantity) {
        SweetbookApiResponse<Map<String, Object>> response;
        try {
            response = sweetbookRestClient.post()
                    .uri("/v1/orders/estimate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("items", List.of(Map.of("bookUid", bookUid, "quantity", quantity))))
                    .retrieve()
                    .body(MAP_RESPONSE_TYPE);
        } catch (RestClientException e) {
            BusinessException be = new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to estimate order.");
            be.initCause(e);
            throw be;
        }

        if (response == null || !response.success() || response.data() == null) {
            throw new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to estimate order.");
        }
        return response.data();
    }

    public Map<String, Object> cancelOrder(String orderUid, String cancelReason) {
        SweetbookApiResponse<Map<String, Object>> response;
        try {
            response = sweetbookRestClient.post()
                    .uri("/v1/orders/{orderUid}/cancel", orderUid)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("cancelReason", cancelReason))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientException e) {
            BusinessException be = new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to cancel order.");
            be.initCause(e);
            throw be;
        }

        if (response == null || !response.success() || response.data() == null) {
            throw new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to cancel order.");
        }
        return response.data();
    }

    public Map<String, Object> updateShipping(String orderUid, Map<String, Object> shippingPatch) {
        SweetbookApiResponse<Map<String, Object>> response;
        try {
            response = sweetbookRestClient.patch()
                    .uri("/v1/orders/{orderUid}/shipping", orderUid)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(shippingPatch)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientException e) {
            BusinessException be = new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to update order shipping.");
            be.initCause(e);
            throw be;
        }

        if (response == null || !response.success() || response.data() == null) {
            throw new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to update order shipping.");
        }
        return response.data();
    }
}
