package com.sweetbook.server.auth.dto;

public record AuthTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}

