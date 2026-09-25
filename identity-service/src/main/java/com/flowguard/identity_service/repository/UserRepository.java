package com.flowguard.identity_service.repository;

import com.flowguard.identity_service.entity.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserRepository extends ReactiveCrudRepository<User, UUID> {
  Mono<User> findByOrganizationIdAndEmail(UUID organizationId, String email);

  /**
   *Because the FlowGuard is multi-tenant, even though the user ID comes from verified JWT, keeping tenant ownership
   * in repository queries gives us another boundary. So, findById(userId) is not enough, we need to also check the organizationId.
   */
  Mono<User> findByIdAndOrganizationId(UUID id, UUID organizationId);

  Flux<User> findAllByOrganizationId(UUID organizationId);


//  Mono<Boolean> existsByOrganizationIdAndEmail(UUID organizationId, String email);
}
