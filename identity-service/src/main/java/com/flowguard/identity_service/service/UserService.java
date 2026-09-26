package com.flowguard.identity_service.service;

import com.flowguard.identity_service.dto.CreateUserRequest;
import com.flowguard.identity_service.entity.Role;
import com.flowguard.identity_service.entity.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserService {
  Mono<User> createUser(UUID organizationId, CreateUserRequest request);
  Mono<User> getCurrentUser(UUID userId, UUID organizationId);
  Flux<User> getUsersByOrganizationId(UUID organizationId);
  //This prevents an OWNER from organization A from assigning roles to a user in organization B.
  Mono<User> assignRoleToUser(UUID userId, UUID organizationId, Role roleName);
  Mono<User> removeRoleFromUser(UUID userId, UUID organizationId, Role roleName);
}
