package com.flowguard.identity_service.dto;

import com.flowguard.identity_service.entity.Role;

import java.util.Set;
import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UUID userId,
        UUID organizationId,
        String email,
        Set<Role> roles
) {
}
