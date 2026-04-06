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
        @NotEmpty(message = "items??理쒖냼 1媛??댁긽?댁뼱???⑸땲??")
        List<@Valid @NotNull Item> items,
        @NotNull(message = "shipping? ?꾩닔?낅땲??")
        @Valid Shipping shipping,
        @Size(max = 100, message = "externalRef??理쒕? 100?먯엯?덈떎.")
        String externalRef,
        @Size(max = 100, message = "externalUserId??理쒕? 100?먯엯?덈떎.")
        String externalUserId
) {

    public record Item(
            @NotBlank(message = "bookUid???꾩닔?낅땲??")
            String bookUid,
            @NotNull(message = "quantity???꾩닔?낅땲??")
            @Min(value = 1, message = "quantity??1 ?댁긽?댁뼱???⑸땲??")
            @Max(value = 100, message = "quantity??100 ?댄븯?ъ빞 ?⑸땲??")
            Integer quantity
    ) {
    }

    public record Shipping(
            @NotBlank(message = "recipientName? ?꾩닔?낅땲??")
            @Size(max = 100, message = "recipientName? 理쒕? 100?먯엯?덈떎.")
            String recipientName,
            @NotBlank(message = "recipientPhone? ?꾩닔?낅땲??")
            @Size(max = 20, message = "recipientPhone? 理쒕? 20?먯엯?덈떎.")
            String recipientPhone,
            @NotBlank(message = "postalCode???꾩닔?낅땲??")
            @Size(max = 10, message = "postalCode??理쒕? 10?먯엯?덈떎.")
            String postalCode,
            @NotBlank(message = "address1? ?꾩닔?낅땲??")
            @Size(max = 200, message = "address1? 理쒕? 200?먯엯?덈떎.")
            String address1,
            @Size(max = 200, message = "address2??理쒕? 200?먯엯?덈떎.")
            String address2,
            @Size(max = 200, message = "memo??理쒕? 200?먯엯?덈떎.")
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

