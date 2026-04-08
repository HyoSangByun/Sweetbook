package com.sweetbook.server.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOrderShippingByUidRequest(
        @NotBlank(message = "recipientName은 필수입니다.")
        @Size(max = 100, message = "recipientName은 최대 100자입니다.")
        String recipientName,
        @Size(max = 10, message = "postalCode는 최대 10자입니다.")
        String postalCode,
        @NotBlank(message = "address1은 필수입니다.")
        @Size(max = 200, message = "address1은 최대 200자입니다.")
        String address1,
        @Size(max = 200, message = "address2는 최대 200자입니다.")
        String address2
) {
}
