package com.sweetbook.server.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpirationSeconds
) {

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("app.jwt.secret 는 null 또는 공백일 수 없습니다.");
        }
        if (secret.length() < 32) {
            throw new IllegalArgumentException("app.jwt.secret 는 최소 32자 이상이어야 합니다.");
        }
        if (accessTokenExpirationSeconds <= 0) {
            throw new IllegalArgumentException("app.jwt.access-token-expiration-seconds 는 0보다 커야 합니다.");
        }
    }
}
