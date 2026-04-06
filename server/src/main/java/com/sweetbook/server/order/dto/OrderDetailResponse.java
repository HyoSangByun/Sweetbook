package com.sweetbook.server.order.dto;

import com.sweetbook.server.order.domain.OrderStatus;
import java.time.LocalDateTime;
import java.util.Map;

public record OrderDetailResponse(
        Long orderId,
        String orderUid,
        String externalRef,
        OrderStatus status,
        String lastErrorMessage,
        Integer remoteOrderStatusCode,
        String remoteOrderStatusDisplay,
        LocalDateTime remoteOrderedAt,
        Map<String, Object> payload,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
