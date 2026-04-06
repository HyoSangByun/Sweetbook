package com.sweetbook.server.sweetbook.dto.orders;

import java.util.Map;

public record CreateOrderRequest(
        Map<String, Object> payload
) {
}

