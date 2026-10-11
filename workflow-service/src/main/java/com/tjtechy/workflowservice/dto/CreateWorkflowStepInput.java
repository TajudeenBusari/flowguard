package com.tjtechy.workflowservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWorkflowStepInput(
        @NotBlank(message = "Step name is required")
        @Size(max = 150, message = "Step name must not exceed 150 characters")
        String name
) {
}
