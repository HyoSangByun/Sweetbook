package com.sweetbook.server.order.dto;

import com.sweetbook.server.order.domain.OrderStatus;
import java.time.LocalDateTime;

public record CreateOrderApiResponse(
        Long orderId,
        String orderUid,
        String externalRef,
        OrderStatus status,
        String lastErrorMessage,
        Integer remoteOrderStatusCode,
        String remoteOrderStatusDisplay,
        LocalDateTime remoteOrderedAt,
        LocalDateTime createdAt
) {
}
