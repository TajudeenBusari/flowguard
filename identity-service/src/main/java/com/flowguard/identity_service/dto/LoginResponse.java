package com.flowguard.identity_service.dto;

import com.flowguard.identity_service.entity.Role;

import java.util.Set;
import java.util.UUID;

/**
 * accessToken
 *     → JWT
 *     → 15-minute lifetime
 *     → sent with normal API requests

 * refreshToken
 *     → opaque random token
 *     → 7-day lifetime
 *     → used only to obtain new access tokens
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UUID userId,
        UUID organizationId,
        String email,
        Set<Role> roles
) {
}
