package com.flowguard.identity_service.security;

import com.flowguard.identity_service.dto.LoginRequest;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

public interface AuthenticationService {
  Mono<Authentication> authenticate(LoginRequest request);
}
