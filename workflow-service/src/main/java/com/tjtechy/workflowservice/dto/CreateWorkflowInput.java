package com.tjtechy.workflowservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * there is deliberately no organizationId in the input. Just like our query,
 * tenant ownership will come from the authenticated JWT
 */
public record CreateWorkflowInput(
        @NotBlank(message = "Workflow is required")
        @Size(max = 150, message = "Workflow name must not exceed 150 characters")
        String name,
        String description
) {


}
