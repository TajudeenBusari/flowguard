package com.flowguard.identity_service.service;

import com.flowguard.identity_service.entity.RefreshSession;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 *createRefreshToken(userId)
 *         ↓
 * generate random token
 *         ↓
 * hash token
 *         ↓
 * store HASH in PostgreSQL
 *         ↓
 * return RAW token to caller
 */
public interface RefreshTokenService {

  Mono<String> createRefreshToken(UUID userId);

  /**
   * raw refresh token
   *       ↓
   * SHA-256
   *       ↓
   * findByTokenHash(...)
   *       ↓
   * session exists?
   *       ↓
   * not revoked?
   *       ↓
   * not expired?
   *       ↓
   * return RefreshSession
   */
  Mono<RefreshSession> validateRefreshToken(String rawRefreshToken);
}
