package com.flowguard.identity_service.dto;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponseDto(
        UUID id,
        String name,
        Instant createdAt
) {
}
