package com.flowguard.identity_service.service;

import com.flowguard.identity_service.dto.*;
import reactor.core.publisher.Mono;

public interface AuthService {
  Mono<LoginResponse> login(LoginRequest request);
  Mono<RefreshTokenResponse> refreshToken(RefreshTokenRequest request);
  Mono<Void> logout(LogoutRequest request);
}
