package com.flowguard.identity_service.repository;

import com.flowguard.identity_service.entity.RefreshSession;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface RefreshSessionRepository extends ReactiveCrudRepository<RefreshSession, UUID> {
  //no longer used
  //Mono<RefreshSession> findByTokenHash(String tokenHash);

  Flux<RefreshSession> findAllByUserId(UUID userId);

  //locking repository query to prevent race condition
  //why Update? Because we want to lock the row for update,
  // so that no other transaction can modify it until the current transaction is complete.
  // This is important in scenarios where we want to ensure that a refresh token is only used once, and not concurrently by multiple requests.
  //Then request 1 can revoke A and commit. Request 2 can subsequently observe that A has already been revoked and reject it.
  @Query("""
    SELECT * FROM refresh_sessions
    WHERE token_hash = :tokenHash
    FOR UPDATE
    """)
  Mono<RefreshSession> findByTokenHashForUpdate(String tokenHash);

  //delete all refresh sessions that have expired before the given cutoff instant
  Mono<Long> deleteByExpiresAtBefore(Instant cutoff);
}
