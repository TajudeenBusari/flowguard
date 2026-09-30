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
   * Refresh token A
   *       ↓
   * validate A
   *       ↓
   * load user
   *       ↓
   * Is user ACTIVE?  ← new check
   *       ↓          ↓
   *       yes        no
   *        ↓         ↓
   *     roles       401
   * load CURRENT roles
   *       ↓
   * create new access JWT
   *       ↓
   * transaction:
   *    revoke A
   *    create B
   *       ↓
   * response:
   *    accessToken
   *    refreshToken B
   *    The important structural difference is that we keep everything requiring the session inside:
   *    .flatMap(refreshSession -> ...)
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
            );
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
