package com.flowguard.identity_service.repository;

import com.flowguard.identity_service.entity.Organization;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface OrganizationRepository extends ReactiveCrudRepository<Organization, UUID> {
  Mono<Organization> findBySlug(String slug);
}
