package com.flowguard.identity_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        @Size(max = 255, message = "Email must be less than or equal to 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must be less than or equal to 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must be less than or equal to 100 characters")
        String lastName
) {
}
