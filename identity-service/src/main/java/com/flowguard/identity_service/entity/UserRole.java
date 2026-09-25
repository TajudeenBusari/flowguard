package com.flowguard.identity_service.entity;

import lombok.*;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_roles")


public class UserRole {

  /**
   * The UserRole table uses a composite primary key consisting of userId and role.
   * This design allows for a many-to-many relationship between users and roles,
   * enabling a user to have multiple roles and a role to be assigned to multiple users.
   * The Spring Data R2DBC does not give a convenient JPA-style composite-ID handling, so we
   * will handle role assignment through repository methods that manage the composite key explicitly.
   * That is rather than ReactiveCrudRepository<UserRole, ...>
   */
  private UUID userId;
  private Role role;
}
