package com.tjtechy.workflowservice.dto;

import java.time.Instant;
import java.util.UUID;

public record WorkflowResponse(
        UUID id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
