package com.tjtechy.workflowservice.dto;

import com.tjtechy.workflowservice.entity.WorkflowStepType;

import java.time.Instant;
import java.util.UUID;

public record WorkflowStepResponse(
        UUID id,
        String name,
        WorkflowStepType type,
        Integer position,
        Instant createdAt,
        Instant updatedAt
) {
}
