package com.sweetbook.server.order.dto;

import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;

public record UpdateOrderShippingRequest(
        @Size(max = 100, message = "recipientName은 최대 100자입니다.")
        String recipientName,
        @Size(max = 20, message = "recipientPhone은 최대 20자입니다.")
        String recipientPhone,
        @Size(max = 10, message = "postalCode는 최대 10자입니다.")
        String postalCode,
        @Size(max = 200, message = "address1은 최대 200자입니다.")
        String address1,
        @Size(max = 200, message = "address2는 최대 200자입니다.")
        String address2,
        @Size(max = 200, message = "shippingMemo는 최대 200자입니다.")
        String shippingMemo
) {
    public Map<String, Object> toPatchMap() {
        Map<String, Object> patch = new LinkedHashMap<>();
        putIfNotBlank(patch, "recipientName", recipientName);
        putIfNotBlank(patch, "recipientPhone", recipientPhone);
        putIfNotBlank(patch, "postalCode", postalCode);
        putIfNotBlank(patch, "address1", address1);
        putIfNotBlank(patch, "address2", address2);
        putIfNotBlank(patch, "shippingMemo", shippingMemo);
        return patch;
    }

    private void putIfNotBlank(Map<String, Object> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value.trim());
        }
    }
}

