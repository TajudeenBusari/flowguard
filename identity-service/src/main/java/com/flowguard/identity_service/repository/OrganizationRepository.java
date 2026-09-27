package com.flowguard.identity_service.repository;

import com.flowguard.identity_service.entity.Organization;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface OrganizationRepository extends ReactiveCrudRepository<Organization, UUID> {
  Mono<Organization> findBySlug(String slug);

  //this essentially locks the row for update, preventing other transactions from modifying it until the current transaction is complete
  @Query("SELECT * FROM organizations WHERE id = :organizationId FOR UPDATE")
  Mono<Organization> findByIdForUpdate(UUID organizationId);
}
