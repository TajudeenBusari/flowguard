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
@Table("users")
public class User {

  @Id
  private UUID id;
  private UUID organizationId;
  private String email;
  private String passwordHash;
  private String firstName;
  private String lastName;
  private UserStatus status;
  private Instant createdAt;
  private Instant updatedAt;
}
