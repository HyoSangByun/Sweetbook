package com.sweetbook.server.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record CreateOrderApiRequest(
        @NotEmpty(message = "items는 최소 1개 이상이어야 합니다.")
        List<@Valid @NotNull Item> items,
        @NotNull(message = "shipping은 필수입니다.")
        @Valid Shipping shipping,
        @Size(max = 100, message = "externalRef는 최대 100자입니다.")
        String externalRef,
        @Size(max = 100, message = "externalUserId는 최대 100자입니다.")
        String externalUserId
) {

    public record Item(
            @NotBlank(message = "bookUid는 필수입니다.")
            String bookUid,
            @NotNull(message = "quantity는 필수입니다.")
            @Min(value = 1, message = "quantity는 1 이상이어야 합니다.")
            @Max(value = 100, message = "quantity는 100 이하여야 합니다.")
            Integer quantity
    ) {
    }

    public record Shipping(
            @NotBlank(message = "recipientName은 필수입니다.")
            @Size(max = 100, message = "recipientName은 최대 100자입니다.")
            String recipientName,
            @NotBlank(message = "recipientPhone은 필수입니다.")
            @Size(max = 20, message = "recipientPhone은 최대 20자입니다.")
            String recipientPhone,
            @NotBlank(message = "postalCode는 필수입니다.")
            @Size(max = 10, message = "postalCode는 최대 10자입니다.")
            String postalCode,
            @NotBlank(message = "address1은 필수입니다.")
            @Size(max = 200, message = "address1은 최대 200자입니다.")
            String address1,
            @Size(max = 200, message = "address2는 최대 200자입니다.")
            String address2,
            @Size(max = 200, message = "memo는 최대 200자입니다.")
            String memo
    ) {
        public Map<String, Object> toMap() {
            return Map.of(
                    "recipientName", recipientName,
                    "recipientPhone", recipientPhone,
                    "postalCode", postalCode,
                    "address1", address1,
                    "address2", address2 == null ? "" : address2,
                    "memo", memo == null ? "" : memo
            );
        }
    }
}

