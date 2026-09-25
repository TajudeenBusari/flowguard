package com.flowguard.identity_service.dto;

public record OrganizationBootstrapResponse(
        OrganizationResponseDto organization,
        UserResponseDto owner

) {
}
