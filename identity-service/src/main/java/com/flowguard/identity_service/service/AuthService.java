package com.flowguard.identity_service.service;

import com.flowguard.identity_service.dto.LoginRequest;
import com.flowguard.identity_service.dto.LoginResponse;
import reactor.core.publisher.Mono;

public interface AuthService {
  Mono<LoginResponse> login(LoginRequest request);
}
