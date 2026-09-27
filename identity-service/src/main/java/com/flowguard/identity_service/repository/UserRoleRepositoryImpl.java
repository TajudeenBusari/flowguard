package com.flowguard.identity_service.repository;

import com.flowguard.identity_service.entity.Role;
import com.flowguard.identity_service.entity.UserRole;
import com.flowguard.identity_service.entity.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRoleRepositoryImpl implements UserRoleRepository {

  /**
   * Why DatabaseClient?
   * Our table has primary key useId and role and neither column
   * alone represents a unique identifier for the UserRole entity. Therefore,
   * we cannot use ReactiveCrudRepository<UserRole, UUID> because it requires a single unique identifier for the entity.
   */
  private final DatabaseClient databaseClient;

  @Override
  public Mono<UserRole> save(UUID userId, Role role) {
    return databaseClient.sql("""
            INSERT INTO user_roles (user_id, role) VALUES ($1, $2)
            """)
            .bind(0, userId).bind(1, role.name()).fetch().rowsUpdated().thenReturn(
                    UserRole.builder()
                            .userId(userId)
                            .role(role)
                            .build()
            );
  }

  @Override
  public Flux<UserRole> findAllByUserId(UUID userId) {
    return databaseClient.sql("""
            SELECT user_id, role FROM user_roles WHERE user_id = :userId
            """)
            .bind("userId", userId)
            .map((row, metadata) -> UserRole.builder()
                    .userId(row.get("user_id", UUID.class))
                    .role(Role.valueOf(row.get("role", String.class)))
                    .build()
            )
            .all();
  }

  @Override
  public Mono<Boolean> existsByUserIdAndRole(UUID userId, Role role) {
    return databaseClient.sql("""
            SELECT EXISTS(SELECT 1 FROM user_roles WHERE user_id = :userId AND role = :role) AS role_exists
            """)
            .bind(0, userId)
            .bind(1, role.name())
            .map((row, rowMetadata) ->
                    Boolean.TRUE.equals(
                            row.get("role_exists", Boolean.class)
                    )
            )
            .one();
  }

  @Override
  public Mono<Void> deleteByUserIdAndRole(UUID userId, Role role) {
    return databaseClient.sql("""
            DELETE FROM user_roles WHERE user_id = $1 AND role = $2
            """)
            .bind(0, userId)
            .bind(1, role.name())
            .fetch()
            .rowsUpdated()
            .then();
  }

  @Override
  public Mono<Long> countActiveOwnersByOrganizationId(UUID organizationId) {
    return databaseClient.sql("""
            SELECT COUNT(*) AS active_owner_count
            FROM user_roles ur
            JOIN users u ON u.id = ur.user_id
            WHERE u.organization_id = $1
              AND ur.role = $2
              AND u.status = $3
            """)
            .bind(0, organizationId)
            .bind(1, Role.OWNER.name())
            .bind(2, UserStatus.ACTIVE.name())
            .map((row, rowMetadata) -> row.get("active_owner_count", Long.class))
            .one();
  }
}
