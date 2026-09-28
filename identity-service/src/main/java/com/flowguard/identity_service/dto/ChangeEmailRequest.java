package com.flowguard.identity_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Requiring current password is appropriate here because changing the logon identifier (email)
 * is a sensitive operation and should be protected by the current password.
 * This ensures that even if someone gains access to the user's session,
 * they cannot change the email without knowing the current password.
 */
public record ChangeEmailRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        String newEmail,
        @NotBlank(message = "Current password is required")
        String currentPassword
) {
}
