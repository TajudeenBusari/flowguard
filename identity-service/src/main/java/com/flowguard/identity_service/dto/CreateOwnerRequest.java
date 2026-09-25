package com.flowguard.identity_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOwnerRequest(
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 255)
        String password,

        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName
) {
}
