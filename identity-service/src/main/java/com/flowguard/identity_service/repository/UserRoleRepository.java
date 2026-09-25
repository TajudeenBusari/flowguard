package com.flowguard.identity_service.repository;

import com.flowguard.identity_service.entity.Role;
import com.flowguard.identity_service.entity.UserRole;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserRoleRepository {

  Mono<UserRole> save(UUID userId, Role role);
  Flux<UserRole> findAllByUserId(UUID userId);
  Mono<Boolean> existsByUserIdAndRole(UUID userId, Role role);
}
