package com.flowguard.identity_service.dto;

import com.flowguard.identity_service.entity.Role;
import com.flowguard.identity_service.entity.UserStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponseDto(
        UUID id,
        String email,
        String firstName,
        String lastName,
        UserStatus status,
        Instant createdAt,
        Set<Role> roles

) {
}
