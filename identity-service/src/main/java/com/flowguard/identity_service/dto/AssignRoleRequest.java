package com.flowguard.identity_service.dto;

import com.flowguard.identity_service.entity.Role;
import jakarta.validation.constraints.NotNull;

/**
 * This only contain the Role to be assigned.
 * OrganizationId will come from the authenticated JWT token
 */
public record AssignRoleRequest(
        @NotNull(message = "Role is required")
        Role role
) {
}
