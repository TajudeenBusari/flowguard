package com.flowguard.identity_service.service;

import com.flowguard.identity_service.dto.LoginRequest;
import com.flowguard.identity_service.dto.LoginResponse;
import com.flowguard.identity_service.dto.RefreshTokenRequest;
import com.flowguard.identity_service.dto.RefreshTokenResponse;
import reactor.core.publisher.Mono;

public interface AuthService {
  Mono<LoginResponse> login(LoginRequest request);
  Mono<RefreshTokenResponse> refreshToken(RefreshTokenRequest request);
}
