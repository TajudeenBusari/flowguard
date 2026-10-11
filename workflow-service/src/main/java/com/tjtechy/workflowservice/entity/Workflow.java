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
@Table("workflows")
public class Workflow {
  @Id
  private UUID Id;
  private UUID organizationId;
  private String name;
  private String description;
  private Instant createdAt;
  private Instant updatedAt;
}
