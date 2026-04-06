package com.sweetbook.server.sweetbook.webhook;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
import com.sweetbook.server.order.service.OrderService;
import com.sweetbook.server.sweetbook.config.SweetbookProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
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
        List<Map<String, Object>> events = parseBodyAsEvents(rawBody);
        for (Map<String, Object> eventPayload : events) {
            String orderUid = getString(eventPayload, "orderUid");
            if (orderUid == null || orderUid.isBlank()) {
                log.info(
                        "Sweetbook webhook ignored: orderUid not found. headerEventType={}, deliveryId={}",
                        eventType,
                        deliveryId
                );
                continue;
            }

            String payloadEvent = getString(eventPayload, "event");
            String resolvedEventType = (payloadEvent != null && !payloadEvent.isBlank()) ? payloadEvent : eventType;
            String status = getString(eventPayload, "status");
            LocalDateTime eventAt = extractEventTime(eventPayload);

            orderService.applyWebhookStatusUpdateByEvent(
                    orderUid,
                    resolvedEventType,
                    status,
                    eventAt,
                    deliveryId
            );
        }
    }

    private void verifyWebhookSignature(String signature, String timestamp, String rawBody) {
        String secret = sweetbookProperties.webhookSecret();
        if (secret == null || secret.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Sweetbook webhook secret is not configured.");
        }
        if (signature == null || timestamp == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Missing webhook signature headers.");
        }

        verifyTimestamp(timestamp);

        String payload = timestamp + "." + rawBody;
        String expected = "sha256=" + hmacSha256Hex(secret, payload);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid webhook signature.");
        }
    }

    private void verifyTimestamp(String timestamp) {
        long epochSeconds;
        try {
            epochSeconds = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid webhook timestamp.");
        }

        Instant eventTime = Instant.ofEpochSecond(epochSeconds);
        Duration drift = Duration.between(eventTime, Instant.now()).abs();
        if (drift.compareTo(sweetbookProperties.webhookTimestampTolerance()) > 0) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Webhook timestamp is out of allowed range.");
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

    private List<Map<String, Object>> parseBodyAsEvents(String rawBody) {
        try {
            Object parsed = objectMapper.readValue(rawBody, new TypeReference<>() {
            });
            if (parsed instanceof List<?> list) {
                return list.stream()
                        .filter(Map.class::isInstance)
                        .map(item -> (Map<String, Object>) item)
                        .toList();
            }
            if (parsed instanceof Map<?, ?> map) {
                Map<String, Object> extracted = extractData((Map<String, Object>) map);
                Object nestedEvents = extracted.get("events");
                if (nestedEvents instanceof List<?> nestedList) {
                    return nestedList.stream()
                            .filter(Map.class::isInstance)
                            .map(item -> (Map<String, Object>) item)
                            .toList();
                }
                return List.of(extracted);
            }
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Invalid webhook body.");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Invalid webhook body.");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractData(Map<String, Object> root) {
        Object data = root.get("data");
        if (data instanceof List<?> list) {
            if (list.isEmpty()) {
                return root;
            }
            Object first = list.get(0);
            if (first instanceof Map<?, ?> firstMap) {
                return (Map<String, Object>) firstMap;
            }
            return root;
        }
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

    private LocalDateTime extractEventTime(Map<String, Object> payload) {
        for (String key : List.of(
                "deliveredAt",
                "shippedAt",
                "completedAt",
                "startedAt",
                "confirmedAt",
                "restoredAt",
                "cancelledAt",
                "orderedAt",
                "timestamp"
        )) {
            LocalDateTime parsed = orderService.parseOrderedAt(getString(payload, key));
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }
}
