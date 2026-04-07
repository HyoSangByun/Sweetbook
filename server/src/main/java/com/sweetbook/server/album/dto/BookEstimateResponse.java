package com.sweetbook.server.album.dto;

public record BookEstimateResponse(
        int estimatedPageCount,
        Long productAmount,
        Long shippingFee,
        Long packagingFee,
        Long totalAmount,
        String currency
) {
}
