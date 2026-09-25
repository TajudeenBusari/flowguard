package com.flowguard.identity_service.repository;

import com.flowguard.identity_service.entity.Role;
import com.flowguard.identity_service.entity.UserRole;
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
            SELECT EXISTS(SELECT 1 FROM user_roles WHERE user_id = :userId AND role = :role) AS exists
            """)
            .bind("userId", userId)
            .bind("role", role.name())
            .map((row, rowMetadata) ->
                    Boolean.TRUE.equals(
                            row.get("role_exists", Boolean.class)
                    )
            )
            .one();
  }
}
