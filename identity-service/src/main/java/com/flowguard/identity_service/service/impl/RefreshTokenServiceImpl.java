package com.flowguard.identity_service.service.impl;

import com.flowguard.identity_service.config.RsaKeyProperties;
import com.flowguard.identity_service.entity.RefreshSession;
import com.flowguard.identity_service.exception.InvalidRefreshTokenException;
import com.flowguard.identity_service.repository.RefreshSessionRepository;
import com.flowguard.identity_service.security.RefreshTokenGenerator;
import com.flowguard.identity_service.security.RefreshTokenHasher;
import com.flowguard.identity_service.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
  private final RefreshSessionRepository refreshSessionRepository;
  private final RefreshTokenGenerator refreshTokenGenerator;
  private final RefreshTokenHasher refreshTokenHasher;
  private final RsaKeyProperties rsaKeyProperties;
  private final TransactionalOperator transactionalOperator;

  /**
   * createRefreshToken(userId)
   *         ↓
   * generate random token
   *         ↓
   * hash token
   *         ↓
   * store HASH in PostgreSQL
   *         ↓
   * return RAW token to caller
   */
  @Override
  public Mono<String> createRefreshToken(UUID userId) {
    return Mono.defer(() -> {
      String rawRefreshToken = refreshTokenGenerator.generateToken();
      String tokenHash = refreshTokenHasher.hash(rawRefreshToken);
      Instant now = Instant.now();
      RefreshSession refreshSession = RefreshSession.builder()
              .userId(userId)
              .tokenHash(tokenHash)
              .expiresAt(now.plus(rsaKeyProperties.refreshTokenExpiration()))
              .createdAt(now)
              .build();
      return refreshSessionRepository
              .save(refreshSession)
              .thenReturn(rawRefreshToken);
    });
  }

  /**
   *raw token
   *    ↓
   * SHA-256
   *    ↓
   * findByTokenHash
   *    ↓
   * not found ───────────────► reject
   *    ↓
   * revokedAt != null ───────► reject
   *    ↓
   * expiresAt <= now ────────► reject
   *    ↓
   * valid RefreshSession
   */
  @Override
  public Mono<RefreshSession> validateRefreshToken(String rawRefreshToken) {

    return Mono.defer(() -> {

      String tokenHash = refreshTokenHasher.hash(rawRefreshToken);

      //System.out.println("Refresh token hash: " + tokenHash);

      return refreshSessionRepository.findByTokenHash(tokenHash)

              .switchIfEmpty(
                      Mono.error(new InvalidRefreshTokenException()))

              .flatMap(refreshSession -> {
                Instant now = Instant.now();
                if (refreshSession.getRevokedAt() != null){
                  return Mono.error(new InvalidRefreshTokenException());
                }
                if (!refreshSession.getExpiresAt().isAfter(now)){
                  return Mono.error(new InvalidRefreshTokenException());
                }
                return Mono.just(refreshSession);
              });
    });
  }

  @Override
  public Mono<Void> revokeRefreshSession(RefreshSession refreshSession) {
    refreshSession.setRevokedAt(Instant.now());
    return refreshSessionRepository.save(refreshSession).then();
  }

  /**
   * BEGIN
   *   ↓
   * revoke old session A
   *   ↓
   * create new session B
   *   ↓
   * both succeed?
   *   ├─ YES → COMMIT
   *   └─ NO  → ROLLBACK
   */
  @Override
  public Mono<String> rotateRefreshToken(RefreshSession refreshSession) {
    return revokeRefreshSession(refreshSession)
            .then(createRefreshToken(refreshSession.getUserId()))
            .as(transactionalOperator::transactional);
  }

  /**
   * userId
   *   ↓
   * find all refresh sessions
   *   ↓
   * keep active sessions
   *   ↓
   * set revokedAt = now
   *   ↓
   * save them
   */
  @Override
  public Mono<Void> revokeAllRefreshSessionsForUser(UUID userId) {
    return Mono.defer(() -> {
      Instant now = Instant.now();
      return refreshSessionRepository.findAllByUserId(userId)
              //This deliberately ignores sessions already revoked
              .filter(refreshSession -> refreshSession.getRevokedAt() == null)
              .flatMap(refreshSession -> {
                refreshSession.setRevokedAt(now);
                return refreshSessionRepository.save(refreshSession);
              }).then();
    });
  }

}
