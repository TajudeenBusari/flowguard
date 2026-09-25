package com.flowguard.identity_service.entity;

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
@Table("organizations")
public class Organization {

  @Id
  private UUID id;
  private String name;
  private String slug;
  private Instant createdAt;
  private Instant updatedAt;
}
