package com.flowguard.identity_service.service.impl;

import com.flowguard.identity_service.config.RsaKeyProperties;
import com.flowguard.identity_service.dto.LoginRequest;
import com.flowguard.identity_service.dto.LoginResponse;
import com.flowguard.identity_service.dto.RefreshTokenRequest;
import com.flowguard.identity_service.dto.RefreshTokenResponse;
import com.flowguard.identity_service.entity.User;
import com.flowguard.identity_service.entity.UserRole;
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

    return refreshTokenService.validateRefreshToken(request.refreshToken())
            .flatMap(refreshSession -> userRepository

                    .findById(refreshSession.getUserId())

                    .switchIfEmpty(
                            Mono.<User>error(new InvalidRefreshTokenException()))

            .flatMap(user -> userRoleRepository
                    .findAllByUserId(user.getId())

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
}
