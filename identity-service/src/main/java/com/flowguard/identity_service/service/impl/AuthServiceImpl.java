package com.flowguard.identity_service.service.impl;

import com.flowguard.identity_service.config.RsaKeyProperties;
import com.flowguard.identity_service.dto.*;
import com.flowguard.identity_service.entity.User;
import com.flowguard.identity_service.entity.UserRole;
import com.flowguard.identity_service.entity.UserStatus;
import com.flowguard.identity_service.exception.InvalidRefreshTokenException;
import com.flowguard.identity_service.repository.UserRepository;
import com.flowguard.identity_service.repository.UserRoleRepository;
import com.flowguard.identity_service.security.AuthenticationService;
import com.flowguard.identity_service.security.FlowGuardPrincipal;
import com.flowguard.identity_service.security.JwtTokenService;
import com.flowguard.identity_service.service.AuthService;
import com.flowguard.identity_service.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  //todo: the expiry is currently duplicated in both JwtTokenService and AuthServiceImpl.
  // we will move into a configuration so there is a single source of truth.
  //DONE
  //private static final long ACCESS_TOKEN_EXPIRES_IN = 7200;

  private final AuthenticationService authenticationService;

  private final RefreshTokenService refreshTokenService;

  private final JwtTokenService jwtTokenService;
  private final RsaKeyProperties rsaKeyProperties;

  private final UserRepository userRepository;
  private final UserRoleRepository userRoleRepository;

  private final TransactionalOperator transactionalOperator;

  /**
   * LoginRequest -> AuthenticationService -> BCrypt verification
   * -> Authentication -> JwtTokenService -> RSA-signed JWT -> LoginResponse
   */
  @Override
  public Mono<LoginResponse> login(LoginRequest request) {
    return authenticationService
            .authenticate(request)
            .flatMap(authentication -> {
              FlowGuardPrincipal principal = (FlowGuardPrincipal) authentication.getPrincipal();
              var user = principal.getUser();
              String accessToken = jwtTokenService.createToken(authentication);

              return refreshTokenService
                      .createRefreshToken(user.getId())
                      .map(refreshToken ->
                              new LoginResponse(
                                      accessToken,
                                      refreshToken,
                                      "Bearer",
                                      rsaKeyProperties.accessTokenExpiration().getSeconds(),
                                      user.getId(),
                                      user.getOrganizationId(),
                                      user.getEmail(),
                                      principal.getRoles()));
            });
  }

  /**
   * Refresh token flow
   * Refresh token A
   *       ↓
   * BEGIN TRANSACTION
   *       ↓
   * hash refresh token A
   *       ↓
   * SELECT refresh session FOR UPDATE
   *       ↓
   * session row A is locked
   *       ↓
   * validate A:
   *   - session exists
   *   - not revoked
   *   - not expired
   *       ↓
   * load user
   *       ↓
   * Is user ACTIVE?
   *       ↓          ↓
   *      yes         no
   *       ↓           ↓
   * load CURRENT     401
   * roles
   *       ↓
   * create new access JWT
   * using current user + roles
   *       ↓
   * rotate refresh token:
   *   revoke session A
   *   create session B
   *       ↓
   * COMMIT TRANSACTION
   *       ↓
   * release lock on session A
   *       ↓
   * response:
   *   accessToken
   *   refreshToken B

   * Concurrent refresh protection:

   * Request 1                    Request 2
   *     ↓                            ↓
   * SELECT A FOR UPDATE          SELECT A FOR UPDATE
   *     ↓                            ↓
   * lock A                       waits for A
   *     ↓
   * validate A
   *     ↓
   * revoke A
   * create B
   *     ↓
   * COMMIT
   *     ↓
   * release lock
   *                                  ↓
   *                            acquires lock
   *                                  ↓
   *                            sees A revoked
   *                                  ↓
   *                                 401

   * The important structural requirement is that validation,
   * user/role loading, and refresh-token rotation remain inside
   * the same reactive transaction:

   * .validateRefreshToken(...)
   * .flatMap(refreshSession -> ...)
   * .as(transactionalOperator::transactional)

   * This ensures the FOR UPDATE lock remains held until the
   * refresh-token rotation has completed and the transaction commits.
   */
  @Override
  public Mono<RefreshTokenResponse> refreshToken(RefreshTokenRequest request) {

    return refreshTokenService
            .validateRefreshToken(request.refreshToken())
            .flatMap(refreshSession -> userRepository.findById(refreshSession.getUserId())

                            .switchIfEmpty(Mono.<User>error(new InvalidRefreshTokenException()))

                            // a user may have been deactivated after the refresh token was issued,
                            // so we need to check the status and not allow refresh if the user is not active
                            .flatMap(user -> {
                              if (user.getStatus() != UserStatus.ACTIVE){
                                return Mono.<User>error(new InvalidRefreshTokenException());
                              }
                              //else, the user is active, so we can proceed with the refresh token flow
                              return Mono.just(user);
                            })

            .flatMap(user -> userRoleRepository.findAllByUserId(user.getId())

                    .map(UserRole::getRole)

                    .collect(Collectors.toSet())

                    .map(roles -> new FlowGuardPrincipal(user, roles)))

            .flatMap(principal -> {

              String accessToken = jwtTokenService.createToken(principal);
              return refreshTokenService.rotateRefreshToken(refreshSession)
                      .map(newRefreshToken -> new RefreshTokenResponse(
                              accessToken,
                              "Bearer",
                              rsaKeyProperties
                                      .accessTokenExpiration()
                                      .getSeconds(),
                              newRefreshToken
                      ));
                })
            ).as(transactionalOperator::transactional);
  }

  /**
   * Raw refresh token
   *       ↓
   * hash + find session
   *       ↓
   * validate it
   *       ↓
   * revoke that specific session
   * This logs out only that refresh session/device, not every session belonging to the user.
   * An invalid, expired, or already-revoked refresh token will currently produce the same existing:
   * 401 Invalid or expired refresh token
   */
  @Override
  public Mono<Void> logout(LogoutRequest request) {
    return refreshTokenService.validateRefreshToken(request.refreshToken())
            .flatMap(refreshSession ->
                    refreshTokenService.revokeRefreshSession(refreshSession));
    //can also be written as: refreshTokenService::revokeRefreshSession
  }
}
