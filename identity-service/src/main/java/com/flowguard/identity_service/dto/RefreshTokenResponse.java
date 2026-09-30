package com.flowguard.identity_service.dto;

public record RefreshTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken
) {
}
