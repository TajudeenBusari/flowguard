package com.tjtechy.workflowservice.entity;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("workflow_versions")
public class WorkflowVersion {
  @Id
  private UUID id;
  private UUID workflowId;
  private Integer version;
  private WorkflowVersionStatus status;
  private Instant createdAt;
  private Instant updatedAt;
}
