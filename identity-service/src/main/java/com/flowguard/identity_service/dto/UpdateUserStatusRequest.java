package com.flowguard.identity_service.dto;

import com.flowguard.identity_service.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull(message = "User status is required")
        UserStatus status
) {
}
