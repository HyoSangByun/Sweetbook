package com.sweetbook.server.sweetbook.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
import com.sweetbook.server.sweetbook.dto.SweetbookApiResponse;
import com.sweetbook.server.sweetbook.dto.orders.CreateOrderRequest;
import com.sweetbook.server.sweetbook.dto.orders.CreateOrderResponseData;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class SweetbookOrdersClient {

    private static final ObjectMapper CANONICAL_OBJECT_MAPPER = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private final RestClient sweetbookRestClient;

    public String createOrder(CreateOrderRequest request) {
        SweetbookApiResponse<CreateOrderResponseData> response;
        String idempotencyKey = createIdempotencyKey(request);
        try {
            response = sweetbookRestClient.post()
                    .uri("/v1/orders")
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request.payload())
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

    private String createIdempotencyKey(CreateOrderRequest request) {
        try {
            String payloadJson = CANONICAL_OBJECT_MAPPER.writeValueAsString(request.payload());
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(payloadJson.getBytes(StandardCharsets.UTF_8));
            return "order-" + HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            BusinessException be = new BusinessException(
                    ErrorCode.SWEETBOOK_CALL_FAILED,
                    "Failed to create order idempotency key."
            );
            be.initCause(e);
            throw be;
        }
    }
}
