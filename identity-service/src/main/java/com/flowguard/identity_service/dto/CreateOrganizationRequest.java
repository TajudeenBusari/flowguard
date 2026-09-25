package com.flowguard.identity_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(

        @NotBlank(message = "Organization name is required")
        @Size(max = 150, message = "Organization name must not exceed 150 characters")
        String name,

        /*
         * accepts the following types: acme, acme-corp, company123, my-company-2026 etc.
         */
        @NotBlank(message = "Organization slug is required")
        @Size(max = 100, message = "Organization slug must not exceed 100 characters")
        @Pattern(
                regexp = "^[a-zA-Z0-9]+(?:-[a-zA-Z0-9]+)*$",
                message = "Organization slug may contain letters, numbers and hyphens"
        )
        String slug,

        @NotNull
        @Valid
        CreateOwnerRequest owner
) {
}
