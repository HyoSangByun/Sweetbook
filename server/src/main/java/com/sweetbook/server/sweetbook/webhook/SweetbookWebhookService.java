package com.sweetbook.server.sweetbook.webhook;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
import com.sweetbook.server.order.service.OrderService;
import com.sweetbook.server.sweetbook.config.SweetbookProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SweetbookWebhookService {

    private final ObjectMapper objectMapper;
    private final SweetbookProperties sweetbookProperties;
    private final OrderService orderService;

    public void handleOrderWebhook(
            String signature,
            String timestamp,
            String eventType,
            String deliveryId,
            String rawBody
    ) {
        verifyWebhookSignature(signature, timestamp, rawBody);
        Map<String, Object> root = parseBody(rawBody);
        Map<String, Object> data = extractData(root);

        String orderUid = getString(data, "orderUid");
        Integer orderStatus = getInteger(data, "orderStatus");
        String orderStatusDisplay = getString(data, "orderStatusDisplay");
        LocalDateTime orderedAt = orderService.parseOrderedAt(getString(data, "orderedAt"));

        if (orderUid == null || orderUid.isBlank()) {
            log.info("Sweetbook webhook ignored: orderUid not found. eventType={}, deliveryId={}", eventType, deliveryId);
            return;
        }

        orderService.applyWebhookStatusUpdate(
                orderUid,
                orderStatus,
                orderStatusDisplay,
                orderedAt,
                deliveryId,
                eventType
        );
    }

    private void verifyWebhookSignature(String signature, String timestamp, String rawBody) {
        String secret = sweetbookProperties.webhookSecret();
        if (secret == null || secret.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Sweetbook webhook secret is not configured.");
        }
        if (signature == null || timestamp == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Missing webhook signature headers.");
        }

        String payload = timestamp + "." + rawBody;
        String expected = "sha256=" + hmacSha256Hex(secret, payload);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid webhook signature.");
        }
    }

    private String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            BusinessException be = new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to verify webhook signature.");
            be.initCause(e);
            throw be;
        }
    }

    private Map<String, Object> parseBody(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Invalid webhook body.");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractData(Map<String, Object> root) {
        Object data = root.get("data");
        if (data instanceof Map<?, ?> mapData) {
            return (Map<String, Object>) mapData;
        }
        return root;
    }

    private String getString(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private Integer getInteger(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

