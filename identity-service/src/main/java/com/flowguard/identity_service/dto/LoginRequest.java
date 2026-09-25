package com.flowguard.identity_service.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LoginRequest(

        //organizationId + email → user
        @NotNull(message = "Organization ID is required")
        UUID organizationId,

        @NotNull(message = "Email is required")
        String email,

        @NotNull(message = "Password is required")
        String password
) {
}
