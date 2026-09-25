package com.flowguard.identity_service.service.impl;

import com.flowguard.identity_service.config.RsaKeyProperties;
import com.flowguard.identity_service.dto.LoginRequest;
import com.flowguard.identity_service.dto.LoginResponse;
import com.flowguard.identity_service.security.AuthenticationService;
import com.flowguard.identity_service.security.FlowGuardPrincipal;
import com.flowguard.identity_service.security.JwtTokenService;
import com.flowguard.identity_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  //todo: the expiry is currently duplicated in both JwtTokenService and AuthServiceImpl.
  // we will move into a configuration so there is a single source of truth.
  //DONE
  //private static final long ACCESS_TOKEN_EXPIRES_IN = 7200;

  private final AuthenticationService authenticationService;

  private final JwtTokenService jwtTokenService;
  private final RsaKeyProperties rsaKeyProperties;

  /**
   * LoginRequest -> AuthenticationService -> BCrypt verification
   * -> Authentication -> JwtTokenService -> RSA-signed JWT -> LoginResponse
   */
  @Override
  public Mono<LoginResponse> login(LoginRequest request) {
    return authenticationService
            .authenticate(request)
            .map(authentication -> {
              FlowGuardPrincipal principal = (FlowGuardPrincipal) authentication.getPrincipal();
              var user = principal.getUser();
              String accessToken = jwtTokenService.createToken(authentication);

              return new LoginResponse(
                      accessToken,
                      "Bearer",
                      rsaKeyProperties.accessTokenExpiration().getSeconds(),
                      user.getId(),
                      user.getOrganizationId(),
                      user.getEmail(),
                      principal.getRoles());
            });
  }
}
