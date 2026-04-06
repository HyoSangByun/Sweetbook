package com.sweetbook.server.order.dto;

import com.sweetbook.server.order.domain.OrderStatus;
import java.time.LocalDateTime;

public record OrderSummaryResponse(
        Long orderId,
        String orderUid,
        String externalRef,
        OrderStatus status,
        String lastErrorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

