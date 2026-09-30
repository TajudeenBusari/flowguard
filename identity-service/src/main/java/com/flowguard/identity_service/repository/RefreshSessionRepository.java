package com.flowguard.identity_service.repository;

import com.flowguard.identity_service.entity.RefreshSession;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RefreshSessionRepository extends ReactiveCrudRepository<RefreshSession, UUID> {
  Mono<RefreshSession> findByTokenHash(String tokenHash);
}
