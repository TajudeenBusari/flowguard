package com.tjtechy.workflowservice.dto;

import com.tjtechy.workflowservice.entity.WorkflowVersionStatus;

import java.time.Instant;
import java.util.UUID;

public record WorkflowVersionResponse(
        UUID id,
        UUID workflowId,
        Integer version,
        WorkflowVersionStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
