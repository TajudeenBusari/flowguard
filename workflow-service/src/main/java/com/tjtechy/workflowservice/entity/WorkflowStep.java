package com.tjtechy.workflowservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("workflow_steps")
public class WorkflowStep {
  @Id
  private UUID id;
  private UUID workflowVersionId;
  private String name;
  @Column("step_type")
  private WorkflowStepType type;
  private Integer position;
  private Instant createdAt;
  private Instant updatedAt;
}
