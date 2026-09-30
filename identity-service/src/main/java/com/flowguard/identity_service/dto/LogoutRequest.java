package com.flowguard.identity_service.dto;

import jakarta.validation.constraints.NotBlank;

//Logout should revoke the specific refresh session being used, rather than logging the user out from every device.
public record LogoutRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
