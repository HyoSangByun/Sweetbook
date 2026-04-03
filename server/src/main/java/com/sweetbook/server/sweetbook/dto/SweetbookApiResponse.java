package com.sweetbook.server.sweetbook.dto;

import java.util.List;

public record SweetbookApiResponse<T>(
        boolean success,
        String message,
        T data,
        List<String> errors
) {
}

